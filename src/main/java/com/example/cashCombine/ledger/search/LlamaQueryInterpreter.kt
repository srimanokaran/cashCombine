package com.example.cashCombine.ledger.search

import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

/**
 * Optional local-model fallback: asks an OpenAI-compatible chat endpoint for related keywords.
 *
 * The model is only ever allowed to widen the search terms. It never sees transactions, never
 * chooses rows and never produces a total, and every term it returns is validated by
 * [SearchTokens] before use. Any failure (disabled, offline, timeout, unusable output) returns
 * null so the caller keeps the deterministic result.
 */
class LlamaQueryInterpreter(
    private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    private val model: String,
    cacheSize: Int = DEFAULT_CACHE_SIZE,
) : ExpenseQueryInterpreter {

    private val logger = LoggerFactory.getLogger(LlamaQueryInterpreter::class.java)
    private val cache = BoundedCache(cacheSize)

    override fun expand(query: String): List<String>? {
        val key = query.lowercase()
        cache.get(key)?.let { return it }
        return try {
            val content = requestCompletion(query) ?: return null
            val terms = SearchTokens.sanitise(parseTokens(content))
            if (terms.isEmpty()) {
                null
            } else {
                cache.put(key, terms)
                terms
            }
        } catch (ex: Exception) {
            logger.debug("Local model query expansion unavailable: {}", ex.message)
            null
        }
    }

    private fun requestCompletion(query: String): String? {
        val payload = mapOf(
            "model" to model,
            "temperature" to 0,
            "max_tokens" to 64,
            "stream" to false,
            "messages" to listOf(
                mapOf("role" to "system", "content" to SYSTEM_PROMPT),
                mapOf("role" to "user", "content" to query),
            ),
        )
        val body = restClient.post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .body(String::class.java)
            ?: return null
        return objectMapper.readTree(body)
            .path("choices").path(0).path("message").path("content")
            .asText("")
            .ifBlank { null }
    }

    internal fun parseTokens(content: String): List<String> {
        val cleaned = content.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val root = try {
            objectMapper.readTree(cleaned)
        } catch (ex: Exception) {
            return emptyList()
        }
        return tokenNodes(root).map { it.asText("") }.filter { it.isNotBlank() }
    }

    private fun tokenNodes(root: JsonNode): List<JsonNode> {
        val array = if (root.isArray) root else root.path("tokens")
        if (!array.isArray) {
            return emptyList()
        }
        return array.values().toList()
    }

    private class BoundedCache(private val capacity: Int) {

        private val entries = object : LinkedHashMap<String, List<String>>(16, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, List<String>>?
            ): Boolean = size > capacity
        }

        @Synchronized
        fun get(key: String): List<String>? = entries[key]

        @Synchronized
        fun put(key: String, value: List<String>) {
            entries[key] = value
        }
    }

    companion object {
        const val DEFAULT_CACHE_SIZE = 500

        private val SYSTEM_PROMPT = """
            You help search a personal bank ledger. Bank descriptions are uppercase merchant strings.
            Turn the user's search query into related search keywords that might appear in those
            descriptions or in a category name (for example another name for the merchant, the city,
            the airline, the rail pass, the currency).
            Reply with JSON only, no explanation: {"tokens":["tokyo","jr pass","narita"]}
            Rules: at most 8 tokens, each 2-24 characters, lowercase, letters and digits and single
            spaces only. Never include amounts, account numbers, dates or punctuation.
        """.trimIndent()
    }
}
