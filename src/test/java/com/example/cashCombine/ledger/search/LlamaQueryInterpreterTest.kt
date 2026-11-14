package com.example.cashCombine.ledger.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper

class LlamaQueryInterpreterTest {

    private val objectMapper = ObjectMapper()
    private val builder = RestClient.builder().baseUrl("http://localhost:11434/v1")
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val interpreter = LlamaQueryInterpreter(builder.build(), objectMapper, "test-model")

    @Test
    fun `parses and sanitises tokens from a chat completion`() {
        server.expect(requestTo("http://localhost:11434/v1/chat/completions"))
            .andRespond(withSuccess(completion("""{"tokens":["Tokyo","narita","12.00","a"]}"""),
                MediaType.APPLICATION_JSON))

        val terms = interpreter.expand("japan")

        assertThat(terms).containsExactly("tokyo", "narita")
        server.verify()
    }

    @Test
    fun `caches successful expansions per query`() {
        server.expect(requestTo("http://localhost:11434/v1/chat/completions"))
            .andRespond(withSuccess(completion("""{"tokens":["tokyo"]}"""),
                MediaType.APPLICATION_JSON))

        assertThat(interpreter.expand("japan")).containsExactly("tokyo")
        assertThat(interpreter.expand("JAPAN")).containsExactly("tokyo")
        server.verify()
    }

    @Test
    fun `returns null when the model is unreachable or errors`() {
        server.expect(requestTo("http://localhost:11434/v1/chat/completions"))
            .andRespond(withServerError())

        assertThat(interpreter.expand("japan")).isNull()
        server.verify()
    }

    @Test
    fun `tolerates fenced json and a bare token array`() {
        assertThat(interpreter.parseTokens("```json\n[\"tokyo\",\"osaka\"]\n```"))
            .containsExactly("tokyo", "osaka")
        assertThat(interpreter.parseTokens("""{"tokens":["narita"]}"""))
            .containsExactly("narita")
        assertThat(interpreter.parseTokens("no json here")).isEmpty()
    }

    private fun completion(content: String): String =
        """{"choices":[{"message":{"role":"assistant","content":${quoted(content)}}}]}"""

    private fun quoted(value: String): String =
        objectMapper.writeValueAsString(value)
}
