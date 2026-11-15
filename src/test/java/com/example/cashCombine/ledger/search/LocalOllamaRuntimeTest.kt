package com.example.cashCombine.ledger.search

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class LocalOllamaRuntimeTest {
    private val ready = OllamaProbeResult.Ready(true)
    private val unavailable = OllamaProbeResult.Unavailable

    @Test
    fun `already running server is used without launching or owning it`() {
        val launches = AtomicInteger()
        val runtime = LocalOllamaRuntime({ ready }, { launches.incrementAndGet(); FakeProcess() })
        runtime.ensureAvailable()
        runtime.close()
        assertThat(launches.get()).isZero()
    }

    @Test
    fun `starts once waits for readiness and stops owned process at shutdown`() {
        val process = FakeProcess()
        val probes = AtomicInteger()
        val launches = AtomicInteger()
        val runtime = LocalOllamaRuntime(
            { if (probes.incrementAndGet() < 3) unavailable else ready },
            { launches.incrementAndGet(); process }, 1000, 1,
        )
        runtime.ensureAvailable()
        runtime.ensureAvailable()
        assertThat(launches.get()).isEqualTo(1)
        assertThat(process.stops.get()).isZero()
        runtime.close()
        runtime.close()
        assertThat(process.stops.get()).isEqualTo(1)
        assertFailure(LocalOllamaException.Reason.CLOSED) { runtime.ensureAvailable() }
    }

    @Test
    fun `missing model is reported without downloading or starting another server`() {
        val launches = AtomicInteger()
        val runtime = LocalOllamaRuntime({ OllamaProbeResult.Ready(false) }, { launches.incrementAndGet(); FakeProcess() })
        assertFailure(LocalOllamaException.Reason.MODEL_MISSING) { runtime.ensureAvailable() }
        assertThat(launches.get()).isZero()
    }

    @Test
    fun `started server remains owned when the model is missing`() {
        val process = FakeProcess()
        val started = AtomicBoolean()
        val runtime = LocalOllamaRuntime(
            { if (started.get()) OllamaProbeResult.Ready(false) else unavailable },
            { started.set(true); process }, 1000, 1,
        )
        assertFailure(LocalOllamaException.Reason.MODEL_MISSING) { runtime.ensureAvailable() }
        assertThat(process.stops.get()).isZero()
        runtime.close()
        assertThat(process.stops.get()).isEqualTo(1)
    }

    @Test
    fun `missing binary and failed process launch produce generic errors`() {
        val missing = LocalOllamaRuntime({ unavailable }, { throw OllamaBinaryMissingException() })
        assertFailure(LocalOllamaException.Reason.BINARY_NOT_FOUND) { missing.ensureAvailable() }
        val failed = LocalOllamaRuntime({ unavailable }, { throw IOException("sensitive details") })
        assertThatThrownBy { failed.ensureAvailable() }
            .isInstanceOf(LocalOllamaException::class.java)
            .hasMessageNotContaining("sensitive details")
        assertFailure(LocalOllamaException.Reason.START_FAILED) { failed.ensureAvailable() }
    }

    @Test
    fun `externally started server wins race with missing binary`() {
        val probes = AtomicInteger()
        val runtime = LocalOllamaRuntime(
            { if (probes.incrementAndGet() == 1) unavailable else ready },
            { throw OllamaBinaryMissingException() },
        )
        runtime.ensureAvailable()
        runtime.close()
    }

    @Test
    fun `externally started server wins race with process exit`() {
        val probes = AtomicInteger()
        val process = FakeProcess(alive = false)
        val runtime = LocalOllamaRuntime(
            { if (probes.incrementAndGet() < 4) unavailable else ready },
            { process }, 1000, 1,
        )
        runtime.ensureAvailable()
        runtime.close()
        assertThat(probes.get()).isGreaterThanOrEqualTo(4)
        assertThat(process.stops.get()).isEqualTo(1)
    }

    @Test
    fun `dead process and timeout are reported and cleaned up`() {
        val dead = FakeProcess(alive = false)
        val failed = LocalOllamaRuntime({ unavailable }, { dead }, 100, 1)
        assertFailure(LocalOllamaException.Reason.START_FAILED) { failed.ensureAvailable() }
        assertThat(dead.stops.get()).isEqualTo(1)

        val alive = FakeProcess()
        val timedOut = LocalOllamaRuntime({ unavailable }, { alive }, 10, 1)
        assertFailure(LocalOllamaException.Reason.START_TIMEOUT) { timedOut.ensureAvailable() }
        assertThat(alive.stops.get()).isEqualTo(1)
    }

    @Test
    fun `slow startup is retried until the owned server is ready`() {
        val process = FakeProcess()
        val probes = AtomicInteger()
        val launches = AtomicInteger()
        val runtime = LocalOllamaRuntime(
            {
                when (probes.incrementAndGet()) {
                    1 -> unavailable
                    2, 3, 4 -> OllamaProbeResult.Unresponsive
                    else -> ready
                }
            },
            { launches.incrementAndGet(); process }, 1000, 1,
        )
        runtime.ensureAvailable()
        assertThat(probes.get()).isEqualTo(5)
        assertThat(launches.get()).isEqualTo(1)
        assertThat(process.stops.get()).isZero()
        runtime.close()
        assertThat(process.stops.get()).isEqualTo(1)
    }

    @Test
    fun `unresponsive existing server can become ready without launching or owning it`() {
        val probes = AtomicInteger()
        val launches = AtomicInteger()
        val runtime = LocalOllamaRuntime(
            { if (probes.incrementAndGet() < 4) OllamaProbeResult.Unresponsive else ready },
            { launches.incrementAndGet(); FakeProcess() }, 1000, 1,
        )
        runtime.ensureAvailable()
        runtime.close()
        assertThat(launches.get()).isZero()
        assertThat(probes.get()).isEqualTo(4)
    }

    @Test
    fun `unresponsive existing server times out without launching even if later unavailable`() {
        val probes = AtomicInteger()
        val launches = AtomicInteger()
        val runtime = LocalOllamaRuntime(
            { if (probes.incrementAndGet() == 1) OllamaProbeResult.Unresponsive else unavailable },
            { launches.incrementAndGet(); FakeProcess() }, 10, 1,
        )
        assertFailure(LocalOllamaException.Reason.START_TIMEOUT) { runtime.ensureAvailable() }
        runtime.close()
        assertThat(launches.get()).isZero()
    }

    @Test
    fun `managed deployment connects only and never launches a server`() {
        val launches = AtomicInteger()
        val runtime = LocalOllamaRuntime(
            { unavailable }, { launches.incrementAndGet(); FakeProcess() },
            startOwned = false, baseUrl = "http://ollama:11434",
        )
        assertThatThrownBy { runtime.ensureAvailable() }
            .isInstanceOf(LocalOllamaException::class.java)
            .hasMessageContaining("http://ollama:11434")
        assertFailure(LocalOllamaException.Reason.UNREACHABLE) { runtime.ensureAvailable() }
        assertThat(launches.get()).isZero()
    }

    @Test
    fun `managed deployment uses an already running server`() {
        val launches = AtomicInteger()
        val runtime = LocalOllamaRuntime(
            { ready }, { launches.incrementAndGet(); FakeProcess() }, startOwned = false,
        )
        runtime.ensureAvailable()
        runtime.close()
        assertThat(launches.get()).isZero()
    }

    @Test
    fun `previously ready owned server is not stopped by a later readiness timeout`() {
        val process = FakeProcess()
        val state = AtomicInteger()
        val runtime = LocalOllamaRuntime(
            { when (state.get()) {
                0 -> unavailable
                1 -> ready
                else -> OllamaProbeResult.Unresponsive
            } },
            { state.set(1); process }, 10, 1,
        )
        runtime.ensureAvailable()
        state.set(2)
        assertFailure(LocalOllamaException.Reason.START_TIMEOUT) { runtime.ensureAvailable() }
        assertThat(process.stops.get()).isZero()
        runtime.close()
        assertThat(process.stops.get()).isEqualTo(1)
    }

    @Test
    fun `unresponsive owned server eventually times out and only its child is stopped`() {
        val process = FakeProcess()
        val probes = AtomicInteger()
        val runtime = LocalOllamaRuntime(
            { if (probes.incrementAndGet() == 1) unavailable else OllamaProbeResult.Unresponsive },
            { process }, 10, 1,
        )
        assertFailure(LocalOllamaException.Reason.START_TIMEOUT) { runtime.ensureAvailable() }
        assertThat(process.stops.get()).isEqualTo(1)
    }

    @Test
    fun `probe classifies header and body timeouts as unresponsive`() {
        fun connection(headerTimeout: Boolean) = object : HttpURLConnection(URI.create("http://127.0.0.1").toURL()) {
            override fun connect() = Unit
            override fun disconnect() = Unit
            override fun usingProxy() = false
            override fun getResponseCode(): Int {
                if (headerTimeout) throw SocketTimeoutException()
                return 200
            }
            override fun getInputStream(): InputStream = object : InputStream() {
                override fun read(): Int = throw SocketTimeoutException()
            }
        }
        assertThat(HttpOllamaProbe { connection(true) }.check()).isEqualTo(OllamaProbeResult.Unresponsive)
        assertThat(HttpOllamaProbe { connection(false) }.check()).isEqualTo(OllamaProbeResult.Unresponsive)
    }

    @Test
    fun `invalid service is not replaced and startup error cleans up the child`() {
        val launches = AtomicInteger()
        val external = LocalOllamaRuntime({ OllamaProbeResult.Invalid }, { launches.incrementAndGet(); FakeProcess() })
        assertFailure(LocalOllamaException.Reason.SERVER_ERROR) { external.ensureAvailable() }
        assertThat(launches.get()).isZero()

        val probes = AtomicInteger()
        val process = FakeProcess()
        val started = LocalOllamaRuntime(
            { if (probes.incrementAndGet() == 1) unavailable else OllamaProbeResult.Invalid },
            { process },
        )
        assertFailure(LocalOllamaException.Reason.SERVER_ERROR) { started.ensureAvailable() }
        assertThat(process.stops.get()).isEqualTo(1)
    }

    @Test
    fun `concurrent requests share one launch`() {
        val running = AtomicBoolean()
        val launches = AtomicInteger()
        val process = FakeProcess()
        val runtime = LocalOllamaRuntime(
            { if (running.get()) ready else unavailable },
            { launches.incrementAndGet(); Thread.sleep(30); running.set(true); process }, 1000, 1,
        )
        val pool = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        try {
            val tasks = (1..8).map {
                pool.submit { start.await(); runtime.ensureAvailable() }
            }
            start.countDown()
            tasks.forEach { it.get(2, TimeUnit.SECONDS) }
            assertThat(launches.get()).isEqualTo(1)
        } finally {
            pool.shutdownNow()
            runtime.close()
        }
        assertThat(process.stops.get()).isEqualTo(1)
    }

    @Test
    fun `interruption restores interrupt status and cleans up`() {
        val process = FakeProcess()
        val runtime = LocalOllamaRuntime({ unavailable }, { process }, 1000, 10)
        val pool = Executors.newSingleThreadExecutor()
        try {
            val interrupted = pool.submit<Boolean> {
                Thread.currentThread().interrupt()
                try {
                    runtime.ensureAvailable()
                    false
                } catch (ex: LocalOllamaException) {
                    ex.reason == LocalOllamaException.Reason.INTERRUPTED && Thread.currentThread().isInterrupted
                }
            }
            assertThat(interrupted.get(2, TimeUnit.SECONDS)).isTrue()
            assertThat(process.stops.get()).isEqualTo(1)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `tags parser only accepts the local default llama3 model`() {
        val probe = HttpOllamaProbe()
        fun parse(json: String) = probe.parseTags(json.toByteArray())
        assertThat(parse("""{"models":[{"name":"llama3:latest"}]}""")).isEqualTo(ready)
        assertThat(parse("""{"models":[{"name":"llama3"}]}""")).isEqualTo(ready)
        assertThat(parse("""{"models":[{"name":"llama3.1:latest"},{"name":"llama3:cloud"}]}"""))
            .isEqualTo(OllamaProbeResult.Ready(false))
        assertThat(parse("""{"models":[{"name":"llama3:latest","remote_host":"https://example.com","remote_model":"x"}]}"""))
            .isEqualTo(OllamaProbeResult.Ready(false))
        assertThat(parse("""{"models":{}}""")).isEqualTo(OllamaProbeResult.Invalid)
        assertThat(parse("not json")).isEqualTo(OllamaProbeResult.Invalid)
    }

    @Test
    fun `tags parser can check a configured model name`() {
        val probe = HttpOllamaProbe(model = "qwen2.5:7b")
        fun parse(json: String) = probe.parseTags(json.toByteArray())
        assertThat(parse("""{"models":[{"name":"qwen2.5:7b"}]}""")).isEqualTo(ready)
        assertThat(parse("""{"models":[{"name":"llama3:latest"}]}"""))
            .isEqualTo(OllamaProbeResult.Ready(false))
    }

    private fun assertFailure(reason: LocalOllamaException.Reason, action: () -> Unit) {
        assertThatThrownBy(action)
            .isInstanceOf(LocalOllamaException::class.java)
            .extracting { (it as LocalOllamaException).reason }
            .isEqualTo(reason)
    }

    private class FakeProcess(alive: Boolean = true) : OllamaProcess {
        private val alive = AtomicBoolean(alive)
        val stops = AtomicInteger()
        override fun isAlive() = alive.get()
        override fun stop() {
            stops.incrementAndGet()
            alive.set(false)
        }
    }
}
