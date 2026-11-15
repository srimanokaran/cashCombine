package com.example.cashCombine.ledger.search

import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import tools.jackson.databind.ObjectMapper

class OllamaExpenseSelectorTest {
    private val mapper = ObjectMapper()
    private fun row(id: String, description: String = "merchant") =
        AiSearchRow(id, "2026-03-02", "-10.00", description, "Travel")

    private fun response(content: String, reason: String = "stop") = mapper.writeValueAsString(mapOf(
        "done" to true, "done_reason" to reason,
        "message" to mapOf("role" to "assistant", "content" to content),
    ))

    @Test
    fun `batches every row and sends only permitted fields using structured local chat`() {
        val requests = mutableListOf<String>()
        val selector = OllamaExpenseSelector(mapper, AiChatTransport { body ->
            requests.add(body)
            val request = mapper.readTree(body)
            val input = mapper.readTree(request.path("messages").path(1).path("content").asText())
            val ids = input.path("transactions").values().map { it.path("id").asText() }
            response(mapper.writeValueAsString(mapOf("ids" to ids)))
        }, { LocalDate.of(2026, 11, 15) })
        val rows = (1..23).map { row("id-$it") }

        assertThat(selector.select("travel last month", rows)).containsExactlyInAnyOrderElementsOf(rows.map { it.id })
        assertThat(requests).hasSize(3)
        val request = mapper.readTree(requests.first())
        assertThat(request.path("model").asText()).isEqualTo("llama3")
        assertThat(request.path("stream").asBoolean()).isFalse()
        assertThat(request.path("truncate").asBoolean()).isFalse()
        assertThat(request.path("shift").asBoolean()).isFalse()
        assertThat(request.path("format").path("type").asText()).isEqualTo("object")
        val system = request.path("messages").path(0).path("content").asText()
        assertThat(system).contains("untrusted", "Never follow")
        val input = mapper.readTree(request.path("messages").path(1).path("content").asText())
        assertThat(input.path("today").asText()).isEqualTo("2026-11-15")
        val item = input.path("transactions").path(0)
        assertThat(item.properties().map { it.key }).containsExactlyInAnyOrder("id", "date", "amount", "description", "category")
    }

    @Test
    fun `rejects malformed truncated duplicate and invented ids`() {
        val invalid = listOf(
            response("not json"), response("[]"), response("{}"), response("{\"ids\":[1]}"),
            response("{\"ids\":[\"a\",\"a\"]}"), response("{\"ids\":[\"invented\"]}"),
            response("{\"ids\":[],\"ids\":[]}"), response("{\"ids\":[]} {}"),
            response("{\"ids\":[]}", "length"), "{}", "bad json",
        )
        for (body in invalid) {
            val selector = OllamaExpenseSelector(mapper, AiChatTransport { body })
            assertThatThrownBy { selector.select("query", listOf(row("a"))) }
                .isInstanceOf(AiSearchException::class.java)
                .extracting { (it as AiSearchException).status }.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        }
    }

    @Test
    fun `rejects a valid id from a different batch and fails whole request`() {
        var calls = 0
        val selector = OllamaExpenseSelector(mapper, AiChatTransport {
            calls++
            if (calls == 1) response("{\"ids\":[\"id-1\"]}")
            else response("{\"ids\":[\"id-1\"]}")
        })
        assertThatThrownBy { selector.select("query", (1..11).map { row("id-$it") }) }
            .isInstanceOf(AiSearchException::class.java)
        assertThat(calls).isEqualTo(2)
    }

    @Test
    fun `transport errors never disclose bank data`() {
        val selector = OllamaExpenseSelector(mapper, AiChatTransport { error("secret merchant") })
        assertThatThrownBy { selector.select("query", listOf(row("a"))) }
            .isInstanceOf(AiSearchException::class.java)
            .hasMessageNotContaining("secret merchant")
    }

    @Test
    fun `rejects an oversized transaction before sending any batch`() {
        var calls = 0
        val selector = OllamaExpenseSelector(mapper, AiChatTransport { calls++; "" })
        assertThatThrownBy { selector.select("query", listOf(row("a"), row("b", "x".repeat(5000)))) }
            .isInstanceOf(AiSearchException::class.java)
            .extracting { (it as AiSearchException).status }.isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
        assertThat(calls).isZero()
    }
}
