package com.example.cashCombine.ledger.search

import tools.jackson.databind.ObjectMapper
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.SocketTimeoutException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/** Loopback is the safe default; containers may point at their own Ollama service. */
internal const val DEFAULT_BASE_URL = "http://127.0.0.1:11434"

/** Starts only a local Ollama server, on demand. Never pulls a model. */
class LocalOllamaRuntime internal constructor(
    private val probe: OllamaProbe,
    private val launcher: OllamaLauncher,
    private val startupTimeoutMs: Long = 60_000,
    private val pollIntervalMs: Long = 100,
    private val startOwned: Boolean = true,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val model: String = "llama3",
) : AutoCloseable {
    constructor() : this(HttpOllamaProbe(), ProcessOllamaLauncher())

    private var ownedProcess: OllamaProcess? = null
    private var closed = false

    /**
     * Returns only when the local server responds and its default llama3 model is installed.
     * Throws [LocalOllamaException] with a stable [LocalOllamaException.reason] otherwise.
     */
    @Synchronized
    fun ensureAvailable() {
        if (closed) throw failure(LocalOllamaException.Reason.CLOSED)
        when (val result = probe.check()) {
            is OllamaProbeResult.Ready -> return requireModel(result)
            OllamaProbeResult.Invalid -> throw failure(LocalOllamaException.Reason.SERVER_ERROR)
            // A listener exists but may still be starting. Never launch a second server over it.
            OllamaProbeResult.Unresponsive -> return awaitReady(startingOwnedProcess = false)
            OllamaProbeResult.Unavailable -> Unit
        }

        // Compose/managed deployments run Ollama separately; only connect to it.
        if (!startOwned) throw failure(LocalOllamaException.Reason.UNREACHABLE)

        if (ownedProcess?.isAlive() != true) {
            ownedProcess = null
            try {
                ownedProcess = launcher.start()
            } catch (_: OllamaBinaryMissingException) {
                // A different server may have won the race after our first probe.
                return checkAfterStartFailure(LocalOllamaException.Reason.BINARY_NOT_FOUND)
            } catch (_: IOException) {
                return checkAfterStartFailure(LocalOllamaException.Reason.START_FAILED)
            }
        }

        awaitReady(startingOwnedProcess = true)
    }

    private fun awaitReady(startingOwnedProcess: Boolean) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(startupTimeoutMs)
        var exitDeadline: Long? = null
        try {
            while (true) {
                val result = probe.check()
                when (result) {
                    is OllamaProbeResult.Ready -> return requireModel(result)
                    OllamaProbeResult.Invalid -> throw failure(LocalOllamaException.Reason.SERVER_ERROR)
                    OllamaProbeResult.Unresponsive -> exitDeadline = null
                    OllamaProbeResult.Unavailable -> Unit
                }
                val now = System.nanoTime()
                // A competing server can still be binding when our child exits after losing the race.
                // If a listener is responding slowly, allow the full timeout even if our child exited.
                if (startingOwnedProcess && result == OllamaProbeResult.Unavailable &&
                    ownedProcess?.isAlive() != true && exitDeadline == null) {
                    exitDeadline = minOf(deadline, now + TimeUnit.MILLISECONDS.toNanos(500))
                }
                val remainingNanos = (exitDeadline ?: deadline) - now
                if (remainingNanos <= 0) {
                    throw failure(if (exitDeadline == null) LocalOllamaException.Reason.START_TIMEOUT
                        else LocalOllamaException.Reason.START_FAILED)
                }
                TimeUnit.NANOSECONDS.sleep(minOf(remainingNanos, TimeUnit.MILLISECONDS.toNanos(pollIntervalMs)))
            }
        } catch (ex: InterruptedException) {
            if (startingOwnedProcess) stopOwnedProcess()
            Thread.currentThread().interrupt()
            throw failure(LocalOllamaException.Reason.INTERRUPTED)
        } catch (ex: LocalOllamaException) {
            // Never stop an existing server, or a healthy child just missing its model.
            if (startingOwnedProcess && ex.reason != LocalOllamaException.Reason.MODEL_MISSING) stopOwnedProcess()
            throw ex
        }
    }

    private fun checkAfterStartFailure(reason: LocalOllamaException.Reason) {
        when (val result = probe.check()) {
            is OllamaProbeResult.Ready -> requireModel(result)
            OllamaProbeResult.Invalid -> throw failure(LocalOllamaException.Reason.SERVER_ERROR)
            OllamaProbeResult.Unresponsive -> awaitReady(startingOwnedProcess = false)
            OllamaProbeResult.Unavailable -> throw failure(reason)
        }
    }

    private fun requireModel(result: OllamaProbeResult.Ready) {
        if (!result.hasModel) throw failure(LocalOllamaException.Reason.MODEL_MISSING)
    }

    @Synchronized
    override fun close() {
        closed = true
        stopOwnedProcess()
    }

    private fun stopOwnedProcess() {
        val process = ownedProcess ?: return
        ownedProcess = null
        process.stop()
    }

    private fun failure(reason: LocalOllamaException.Reason) = LocalOllamaException(reason, model, baseUrl)
}

/** Errors are generic by design: process output and model/server responses are never exposed. */
class LocalOllamaException(
    val reason: Reason,
    model: String = "llama3",
    baseUrl: String = DEFAULT_BASE_URL,
) : IllegalStateException(
    when (reason) {
        Reason.BINARY_NOT_FOUND -> "Ollama was not found. Install it and ensure ollama is on PATH or in /opt/homebrew/bin."
        Reason.START_FAILED -> "Could not start the local Ollama server. Check your Ollama installation."
        Reason.START_TIMEOUT -> "The local Ollama server did not become ready in time."
        Reason.SERVER_ERROR -> "The local Ollama server returned an invalid response or could not be read."
        Reason.MODEL_MISSING -> "The $model model is not installed. Install it manually with 'ollama pull $model' and try again."
        Reason.UNREACHABLE -> "No Ollama server is reachable at $baseUrl. Start it, then try again."
        Reason.INTERRUPTED -> "Starting the local Ollama server was interrupted."
        Reason.CLOSED -> "The local Ollama runtime has been shut down."
    }
) {
    enum class Reason {
        BINARY_NOT_FOUND, START_FAILED, START_TIMEOUT, SERVER_ERROR, MODEL_MISSING, UNREACHABLE, INTERRUPTED, CLOSED
    }
}

internal sealed interface OllamaProbeResult {
    data class Ready(val hasModel: Boolean) : OllamaProbeResult
    data object Unavailable : OllamaProbeResult
    data object Unresponsive : OllamaProbeResult
    data object Invalid : OllamaProbeResult
}

internal fun interface OllamaProbe {
    fun check(): OllamaProbeResult
}

internal fun interface OllamaLauncher {
    @Throws(IOException::class, OllamaBinaryMissingException::class)
    fun start(): OllamaProcess
}

internal interface OllamaProcess {
    fun isAlive(): Boolean
    fun stop()
}

internal class OllamaBinaryMissingException : Exception()

/** No proxy and no redirects. The host is configurable so a container can reach its Ollama service. */
internal class HttpOllamaProbe(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val model: String = "llama3",
    private val openConnection: () -> HttpURLConnection = {
        URI.create("${baseUrl.trimEnd('/')}/api/tags").toURL()
            .openConnection(Proxy.NO_PROXY) as HttpURLConnection
    },
) : OllamaProbe {
    override fun check(): OllamaProbeResult {
        val connection = openConnection()
        connection.connectTimeout = PROBE_TIMEOUT_MS
        connection.readTimeout = PROBE_TIMEOUT_MS
        connection.instanceFollowRedirects = false
        return try {
            if (connection.responseCode != 200) return OllamaProbeResult.Invalid
            val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(PROBE_TIMEOUT_MS.toLong())
            val bytes = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(4096)
                while (true) {
                    val remaining = deadline - System.nanoTime()
                    if (remaining <= 0) return OllamaProbeResult.Unresponsive
                    connection.readTimeout = TimeUnit.NANOSECONDS.toMillis(remaining).coerceAtLeast(1).toInt()
                    val count = input.read(buffer)
                    if (count < 0) break
                    bytes.write(buffer, 0, count)
                    if (bytes.size() > MAX_RESPONSE_BYTES) return OllamaProbeResult.Invalid
                }
            }
            parseTags(bytes.toByteArray())
        } catch (_: ConnectException) {
            OllamaProbeResult.Unavailable
        } catch (_: SocketTimeoutException) {
            OllamaProbeResult.Unresponsive
        } catch (_: Exception) {
            OllamaProbeResult.Invalid
        } finally {
            connection.disconnect()
        }
    }

    internal fun parseTags(bytes: ByteArray): OllamaProbeResult = try {
        val models = ObjectMapper().readTree(bytes).path("models")
        if (!models.isArray) {
            OllamaProbeResult.Invalid
        } else {
            OllamaProbeResult.Ready(models.values().any {
                val name = it.path("name").asText("")
                (name == model || name == "$model:latest") &&
                    it.path("remote_host").asText("").isEmpty() &&
                    it.path("remote_model").asText("").isEmpty()
            })
        }
    } catch (_: Exception) {
        OllamaProbeResult.Invalid
    }

    private companion object {
        const val PROBE_TIMEOUT_MS = 2_000
        const val MAX_RESPONSE_BYTES = 1024 * 1024
    }
}

internal class ProcessOllamaLauncher(
    private val baseUrl: String = DEFAULT_BASE_URL,
) : OllamaLauncher {
    override fun start(): OllamaProcess {
        val executable = findExecutable() ?: throw OllamaBinaryMissingException()
        val process = ProcessBuilder(executable.toString(), "serve")
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .apply {
                environment()["OLLAMA_HOST"] = hostAndPort()
                environment()["OLLAMA_NO_CLOUD"] = "1"
            }
            .start()
        // No input is sent to the child. A close failure must not lose the process handle.
        try {
            process.outputStream.close()
        } catch (_: IOException) {
            // The process may already have exited.
        }
        return object : OllamaProcess {
            override fun isAlive() = process.isAlive
            override fun stop() {
                if (!process.isAlive) return
                process.destroy()
                try {
                    if (!process.waitFor(1, TimeUnit.SECONDS)) process.destroyForcibly()
                } catch (_: InterruptedException) {
                    process.destroyForcibly()
                    Thread.currentThread().interrupt()
                }
            }
        }
    }

    private fun findExecutable(): Path? {
        val paths = (System.getenv("PATH") ?: "").split(File.pathSeparator)
            .filter { it.isNotBlank() }
            .map { Path.of(it).resolve("ollama") } +
            listOf(Path.of("/opt/homebrew/bin/ollama"), Path.of("/usr/local/bin/ollama"))
        return paths.firstOrNull { Files.isRegularFile(it) && Files.isExecutable(it) }?.toAbsolutePath()
    }

    /** Ollama wants host:port rather than a URL. */
    private fun hostAndPort(): String {
        val uri = URI.create(baseUrl)
        val port = if (uri.port == -1) 11434 else uri.port
        return "${uri.host ?: "127.0.0.1"}:$port"
    }
}
