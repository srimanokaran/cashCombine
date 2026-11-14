package com.example.cashCombine.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExpenseSearchApiIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun searchesImportedExpensesByDescription() {
        val accountId = createAccount("Search Everyday")

        val csv = """
            02/03/2026,"-920.00","JAPAN RAIL PASS 1234","+1000.00"
            05/03/2026,"-210.50","SHIBUYA HOTEL","+789.50"
            06/03/2026,"-45.00","WOOLWORTHS 1234 FAKETOWN VIC","+744.50"
        """.trimIndent().toByteArray(Charsets.UTF_8)
        mockMvc.perform(
            multipart("/api/accounts/$accountId/import")
                .file(MockMultipartFile("file", "japan.csv", "text/csv", csv))
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/search/expenses").param("q", "japan rail"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.query").value("japan rail"))
            .andExpect(jsonPath("$.expenseCount").value(1))
            .andExpect(jsonPath("$.totalSpend").value(920.0))
            .andExpect(jsonPath("$.transactions.length()").value(1))
            .andExpect(jsonPath("$.transactions[0].description").value("JAPAN RAIL PASS 1234"))
            .andExpect(jsonPath("$.transactions[0].matchedBy").value("QUERY"))
            .andExpect(jsonPath("$.suggestions[0].label").value("JAPAN RAIL PASS"))

        mockMvc.perform(get("/api/search/expenses").param("q", "shibuya"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.expenseCount").value(1))
            .andExpect(jsonPath("$.firstDate").value("2026-03-05"))
            .andExpect(jsonPath("$.lastDate").value("2026-03-05"))

        mockMvc.perform(get("/api/search/expenses").param("q", "kyoto"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.expenseCount").value(0))
            .andExpect(jsonPath("$.totalSpend").value(0.0))
            .andExpect(jsonPath("$.transactions.length()").value(0))
    }

    @Test
    fun matchesCategoryNamesAndExcludesFundsBetweenAccounts() {
        val accountId = createAccount("Search Category")
        val travelId = createCategory("Japan trip integration")

        val csv = """
            02/03/2026,"-920.00","JAPAN RAIL PASS","+1000.00"
            05/03/2026,"-210.50","SHIBUYA HOTEL","+789.50"
            06/03/2026,"-6000.00","Qantas Credit Cards BPAY","+6789.50"
        """.trimIndent().toByteArray(Charsets.UTF_8)
        val importResult = mockMvc.perform(
            multipart("/api/accounts/$accountId/import")
                .file(MockMultipartFile("file", "trip.csv", "text/csv", csv))
        ).andExpect(status().isOk).andReturn()

        val transactions = mockMvc.perform(get("/api/accounts/$accountId/transactions"))
            .andExpect(status().isOk)
            .andReturn()
            .response.contentAsString

        for (fragment in listOf("JAPAN RAIL PASS", "SHIBUYA HOTEL")) {
            val id = extractTransactionIdForDescription(transactions, fragment)
            mockMvc.perform(
                patch("/api/transactions/$id/category")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"categoryId\":\"$travelId\"}")
            ).andExpect(status().isOk)
        }

        mockMvc.perform(get("/api/search/expenses").param("q", "japan trip integration"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.expenseCount").value(2))
            .andExpect(jsonPath("$.totalSpend").value(1130.5))
            .andExpect(jsonPath("$.categories[0].categoryName").value("Japan trip integration"))
            .andExpect(jsonPath("$.categories[0].transactionCount").value(2))

        // The BPAY transfer is a real match but never counts as spend.
        mockMvc.perform(get("/api/search/expenses").param("q", "qantas credit"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.expenseCount").value(0))
            .andExpect(jsonPath("$.excludedCount").value(1))
            .andExpect(jsonPath("$.totalSpend").value(0.0))

        assertThat(importResult.response.status).isEqualTo(200)
    }

    @Test
    fun respectsDateWindowAndLimit() {
        val accountId = createAccount("Search Window")

        val csv = """
            02/03/2026,"-10.00","JAPAN RAIL PASS","+1000.00"
            03/03/2026,"-20.00","JAPAN RAIL PASS","+980.00"
            05/11/2026,"-30.00","JAPAN RAIL PASS","+950.00"
        """.trimIndent().toByteArray(Charsets.UTF_8)
        mockMvc.perform(
            multipart("/api/accounts/$accountId/import")
                .file(MockMultipartFile("file", "window.csv", "text/csv", csv))
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/api/search/expenses")
                .param("q", "japan rail pass")
                .param("from", "2026-03-01")
                .param("to", "2026-03-31")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.expenseCount").value(2))
            .andExpect(jsonPath("$.totalSpend").value(30.0))

        mockMvc.perform(get("/api/search/expenses").param("q", "japan rail pass").param("limit", "2"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.expenseCount").value(3))
            .andExpect(jsonPath("$.transactions.length()").value(2))
            .andExpect(jsonPath("$.truncated").value(true))
    }

    @Test
    fun queryExpansionIsUnavailableWhenTheLocalModelIsDisabled() {
        mockMvc.perform(get("/api/search/expenses/expand").param("q", "japan"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.available").value(false))
            .andExpect(jsonPath("$.query").value("japan"))
            .andExpect(jsonPath("$.terms.length()").value(0))

        mockMvc.perform(get("/api/search/expenses/expand").param("q", "jp"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.available").value(false))
    }

    @Test
    fun rejectsUnsafeExpandParameters() {
        val accountId = createAccount("Search Expand")
        val csv = """
            02/03/2026,"-920.00","JAPAN RAIL PASS","+1000.00"
        """.trimIndent().toByteArray(Charsets.UTF_8)
        mockMvc.perform(
            multipart("/api/accounts/$accountId/import")
                .file(MockMultipartFile("file", "expand.csv", "text/csv", csv))
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/api/search/expenses")
                .param("q", "japan")
                .param("expand", "'); DROP TABLE transactions;--,12.00")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.expandedTerms.length()").value(0))
            .andExpect(jsonPath("$.expenseCount").value(1))
    }

    private fun createAccount(name: String): String {
        val result = mockMvc.perform(
            post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"$name\",\"type\":\"COMMBANK\"}")
        )
            .andExpect(status().isCreated)
            .andReturn()
        return readJsonField(result, "id")
    }

    private fun createCategory(name: String): String {
        val result = mockMvc.perform(
            post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"$name\"}")
        )
            .andExpect(status().isCreated)
            .andReturn()
        return readJsonField(result, "id")
    }

    private fun readJsonField(result: MvcResult, field: String): String {
        val json = result.response.contentAsString
        val marker = "\"$field\":\""
        val start = json.indexOf(marker) + marker.length
        val end = json.indexOf('"', start)
        return json.substring(start, end)
    }

    private fun extractTransactionIdForDescription(jsonArray: String, descriptionFragment: String): String {
        val descIndex = jsonArray.indexOf(descriptionFragment)
        assertThat(descIndex).`as`("description containing %s", descriptionFragment).isPositive
        val objectStart = jsonArray.lastIndexOf('{', descIndex)
        val marker = "\"id\":\""
        val idStart = jsonArray.indexOf(marker, objectStart) + marker.length
        val idEnd = jsonArray.indexOf('"', idStart)
        return jsonArray.substring(idStart, idEnd)
    }
}
