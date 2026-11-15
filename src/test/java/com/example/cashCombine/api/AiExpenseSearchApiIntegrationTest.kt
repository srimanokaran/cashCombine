package com.example.cashCombine.api

import com.example.cashCombine.ledger.search.AiSearchRow
import com.example.cashCombine.ledger.search.AiTransactionSelector
import com.example.cashCombine.ledger.search.LocalOllamaRuntime
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AiExpenseSearchApiIntegrationTest {
    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var runtime: LocalOllamaRuntime
    @MockitoBean private lateinit var selector: AiTransactionSelector

    @Test
    fun `start and search endpoints return the agreed response`() {
        mockMvc.perform(post("/api/search/ai/start"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.available").value(true))
        Mockito.verify(runtime).ensureAvailable()

        val accountResult = mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"AI test\",\"type\":\"COMMBANK\"}"))
            .andExpect(status().isCreated).andReturn().response.contentAsString
        val id = Regex("\"id\":\"([^\"]+)\"").find(accountResult)!!.groupValues[1]
        val csv = "02/03/2026,\"-100.00\",\"TRAVEL HOTEL\",\"1000.00\"\n"
        mockMvc.perform(multipart("/api/accounts/$id/import")
            .file(MockMultipartFile("file", "test.csv", "text/csv", csv.toByteArray())))
            .andExpect(status().isOk)
        Mockito.doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            (invocation.arguments[1] as List<AiSearchRow>).map { it.id }.toSet()
        }.`when`(selector).select(Mockito.anyString(), Mockito.anyList())

        mockMvc.perform(post("/api/search/expenses/ai").contentType(MediaType.APPLICATION_JSON)
            .content("{\"query\":\"travel last year\"}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.query").value("travel last year"))
            .andExpect(jsonPath("$.expenseCount").value(1))
            .andExpect(jsonPath("$.totalSpend").value(100.0))
            .andExpect(jsonPath("$.transactions[0].matchedBy").value("AI"))
            .andExpect(jsonPath("$.truncated").value(false))
    }

    @Test
    fun `rejects invalid query and returns safe error when runtime fails`() {
        mockMvc.perform(post("/api/search/expenses/ai").contentType(MediaType.APPLICATION_JSON)
            .content("{\"query\":\"\"}"))
            .andExpect(status().isBadRequest)
        Mockito.verifyNoInteractions(runtime)
        Mockito.doThrow(IllegalStateException("sensitive bank detail")).`when`(runtime).ensureAvailable()
        mockMvc.perform(post("/api/search/ai/start"))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.message").value(
                "Local AI search is unavailable or returned an invalid response. Please try again."))
    }
}
