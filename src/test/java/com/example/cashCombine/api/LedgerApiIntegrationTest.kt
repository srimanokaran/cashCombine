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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
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
class LedgerApiIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun createImportListAndChangeCategory() {
        val groceriesId = categoryIdByName("Groceries")
        val diningId = categoryIdByName("Dining")

        val accountResult = mockMvc.perform(
            post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Everyday\",\"type\":\"COMMBANK\"}")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Everyday"))
            .andExpect(jsonPath("$.type").value("COMMBANK"))
            .andReturn()
        val accountId = readJsonField(accountResult, "id")

        val csv = """
            10/07/2026,"-45.00","WOOLWORTHS 1234 FAKETOWN VIC AUS","+2455.00"
            10/07/2026,"-12.50","CAFE EXAMPLE BLEND FAKETOWN AUS","+2467.50"
        """.trimIndent().toByteArray(Charsets.UTF_8)
        val file = MockMultipartFile("file", "sample.csv", "text/csv", csv)

        mockMvc.perform(multipart("/api/accounts/$accountId/import").file(file))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").isNotEmpty)
            .andExpect(jsonPath("$.accepted").value(2))
            .andExpect(jsonPath("$.duplicate").value(0))
            .andExpect(jsonPath("$.rejected").value(0))

        mockMvc.perform(get("/api/accounts/$accountId/imports"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].filename").value("sample.csv"))
            .andExpect(jsonPath("$[0].accepted").value(2))

        val txResult = mockMvc.perform(get("/api/accounts/$accountId/transactions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andReturn()

        val body = txResult.response.contentAsString
        assertThat(body).contains("WOOLWORTHS")
        assertThat(body).contains(groceriesId)

        val cafeTxId = extractTransactionIdForDescription(body, "CAFE EXAMPLE")

        mockMvc.perform(
            patch("/api/transactions/$cafeTxId/category")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryId\":\"$diningId\"}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.categoryId").value(diningId))
            .andExpect(jsonPath("$.categoryAssignmentSource").value("MANUAL"))

        val importResult = mockMvc.perform(get("/api/accounts/$accountId/imports"))
            .andExpect(status().isOk)
            .andReturn()
        val importId = readJsonField(importResult, "id")

        mockMvc.perform(delete("/api/accounts/$accountId/imports/$importId"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/accounts/$accountId/imports"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
        mockMvc.perform(get("/api/accounts/$accountId/transactions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
        mockMvc.perform(get("/api/accounts/$accountId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.hasImports").value(false))

        mockMvc.perform(delete("/api/accounts/$accountId")).andExpect(status().isNoContent)
        mockMvc.perform(get("/api/accounts/$accountId")).andExpect(status().isNotFound)
    }

    @Test
    fun createAndDeleteRule() {
        val subscriptionId = categoryIdByName("Subscription")

        val ruleResult = mockMvc.perform(
            post("/api/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pattern\":\"CUSTOM-STREAM-TEST\",\"categoryId\":\"$subscriptionId\"}")
        )
            .andExpect(status().isCreated)
            .andReturn()
        val ruleId = readJsonField(ruleResult, "id")

        mockMvc.perform(get("/api/rules"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[?(@.pattern=='CUSTOM-STREAM-TEST')]").isNotEmpty)

        mockMvc.perform(delete("/api/rules/$ruleId")).andExpect(status().isNoContent)

        mockMvc.perform(get("/api/rules"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[?(@.pattern=='CUSTOM-STREAM-TEST')]").isEmpty)

        mockMvc.perform(delete("/api/rules/$ruleId")).andExpect(status().isNotFound)
    }

    @Test
    fun createAndDeleteCategory() {
        val categoryResult = mockMvc.perform(
            post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Travel\"}")
        )
            .andExpect(status().isCreated)
            .andReturn()
        val categoryId = readJsonField(categoryResult, "id")

        mockMvc.perform(
            post("/api/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pattern\":\"AIRLINE-TEST\",\"categoryId\":\"$categoryId\"}")
        )
            .andExpect(status().isCreated)

        mockMvc.perform(delete("/api/categories/$categoryId")).andExpect(status().isNoContent)

        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[?(@.name=='Travel')]").isEmpty)

        mockMvc.perform(get("/api/rules"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[?(@.pattern=='AIRLINE-TEST')]").isEmpty)

        val uncategorisedId = categoryIdByName("Uncategorised")
        mockMvc.perform(delete("/api/categories/$uncategorisedId")).andExpect(status().isConflict)
    }

    @Test
    fun importsNabCreditCardCsvThroughApi() {
        val accountResult = mockMvc.perform(
            post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Qantas Money\",\"type\":\"NAB_CREDIT_CARD\"}")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.type").value("NAB_CREDIT_CARD"))
            .andReturn()
        val accountId = readJsonField(accountResult, "id")

        val file = MockMultipartFile(
            "file", "qantas-money-sample.csv", "text/csv",
            classpathBytes("/csv/qantas-money-sample.csv")
        )

        mockMvc.perform(multipart("/api/accounts/$accountId/import").file(file))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accepted").value(6))
            .andExpect(jsonPath("$.duplicate").value(1))
            .andExpect(jsonPath("$.rejected").value(0))

        val txResult = mockMvc.perform(get("/api/accounts/$accountId/transactions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(6))
            .andReturn()

        val body = txResult.response.contentAsString
        assertThat(body).contains("WOOLWORTHS 1234 FAKETOWN")
        assertThat(body).contains("BPAY PAYMENT - THANK YOU")
        assertThat(body).contains(categoryIdByName("Groceries"))
    }

    @Test
    fun dashboardCountsCardMerchantsAndExcludesCashCardPayments() {
        val groceriesId = categoryIdByName("Groceries")
        val fundsId = categoryIdByName("Funds between accounts")

        val cashResult = mockMvc.perform(
            post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Everyday\",\"type\":\"COMMBANK\"}")
        )
            .andExpect(status().isCreated)
            .andReturn()
        val cashAccountId = readJsonField(cashResult, "id")

        val cashCsv = """
            15/07/2026,"-6000.00","Qantas Credit Cards CommBank app BPAY 000000 0000000000000000 Bill","+100.00"
            08/07/2026,"-20.00","COLES 0001 FAKETOWN VIC","+6120.00"
        """.trimIndent().toByteArray(Charsets.UTF_8)
        mockMvc.perform(
            multipart("/api/accounts/$cashAccountId/import")
                .file(MockMultipartFile("file", "commbank.csv", "text/csv", cashCsv))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accepted").value(2))

        val cardResult = mockMvc.perform(
            post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Qantas Money\",\"type\":\"NAB_CREDIT_CARD\"}")
        )
            .andExpect(status().isCreated)
            .andReturn()
        val cardAccountId = readJsonField(cardResult, "id")

        val cardCsv = """
            Date,Amount,Account Number,,Transaction Type,Transaction Details,Category,Merchant Name,Processed On
            10 July 26,-45.00,Card ending 9999,,CREDIT CARD PURCHASE,WOOLWORTHS 9999 TESTTOWN,Groceries,Woolworths,10 July 26
            09 July 26,-25.00,Card ending 9999,,CREDIT CARD PURCHASE,EXAMPLE CAFE TESTTOWN,Restaurants,Example Cafe,09 July 26
        """.trimIndent().toByteArray(Charsets.UTF_8)
        mockMvc.perform(
            multipart("/api/accounts/$cardAccountId/import")
                .file(MockMultipartFile("file", "card.csv", "text/csv", cardCsv))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accepted").value(2))

        val cashTx = mockMvc.perform(get("/api/accounts/$cashAccountId/transactions"))
            .andExpect(status().isOk)
            .andReturn()
        val cashBody = cashTx.response.contentAsString
        assertThat(cashBody).contains(fundsId)
        assertThat(extractCategoryIdForDescription(cashBody, "Qantas Credit Cards")).isEqualTo(fundsId)
        assertThat(extractCategoryIdForDescription(cashBody, "COLES")).isEqualTo(groceriesId)

        val dashboard = mockMvc.perform(get("/api/dashboard/expenses"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalExpenses").value(90.0))
            .andExpect(jsonPath("$.expenseTransactionCount").value(3))
            .andExpect(jsonPath("\$[?(@.categoryName=='Funds between accounts')]").isEmpty)
            .andExpect(jsonPath("\$[?(@.categoryName=='Credit cards')]").isEmpty)
            .andReturn()

        val dashboardBody = dashboard.response.contentAsString
        assertThat(dashboardBody).contains("\"categoryName\":\"Groceries\"")
        assertThat(dashboardBody).contains("\"amount\":65.00")
    }

    @Test
    fun reimportingSameCsvThroughApiIsIdempotent() {
        val accountResult = mockMvc.perform(
            post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Everyday\",\"type\":\"COMMBANK\"}")
        )
            .andExpect(status().isCreated)
            .andReturn()
        val accountId = readJsonField(accountResult, "id")

        val csv = """
            10/07/2026,"-45.00","WOOLWORTHS 1234 FAKETOWN VIC AUS","+2455.00"
            10/07/2026,"-12.50","CAFE EXAMPLE BLEND FAKETOWN AUS","+2467.50"
        """.trimIndent().toByteArray(Charsets.UTF_8)
        val first = MockMultipartFile("file", "sample.csv", "text/csv", csv)
        val second = MockMultipartFile("file", "sample-again.csv", "text/csv", csv)

        mockMvc.perform(multipart("/api/accounts/$accountId/import").file(first))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accepted").value(2))
            .andExpect(jsonPath("$.duplicate").value(0))
            .andExpect(jsonPath("$.rejected").value(0))

        mockMvc.perform(multipart("/api/accounts/$accountId/import").file(second))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accepted").value(0))
            .andExpect(jsonPath("$.duplicate").value(2))
            .andExpect(jsonPath("$.rejected").value(0))

        mockMvc.perform(get("/api/accounts/$accountId/transactions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))

        mockMvc.perform(get("/api/accounts/$accountId/imports"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    private fun categoryIdByName(name: String): String {
        val result = mockMvc.perform(get("/api/categories")).andExpect(status().isOk).andReturn()
        val json = result.response.contentAsString
        val needle = "\"name\":\"$name\""
        val nameIndex = json.indexOf(needle)
        assertThat(nameIndex)
            .`as`("category %s should exist", name)
            .isGreaterThanOrEqualTo(0)
        val objectStart = json.lastIndexOf('{', nameIndex)
        val marker = "\"id\":\""
        val idStart = json.indexOf(marker, objectStart) + marker.length
        val idEnd = json.indexOf('"', idStart)
        return json.substring(idStart, idEnd)
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
        assertThat(descIndex).isPositive
        val objectStart = jsonArray.lastIndexOf('{', descIndex)
        val marker = "\"id\":\""
        val idStart = jsonArray.indexOf(marker, objectStart) + marker.length
        val idEnd = jsonArray.indexOf('"', idStart)
        return jsonArray.substring(idStart, idEnd)
    }

    private fun extractCategoryIdForDescription(jsonArray: String, descriptionFragment: String): String {
        val descIndex = jsonArray.indexOf(descriptionFragment)
        assertThat(descIndex).`as`("description containing %s", descriptionFragment).isPositive
        val objectStart = jsonArray.lastIndexOf('{', descIndex)
        val marker = "\"categoryId\":\""
        val idStart = jsonArray.indexOf(marker, objectStart) + marker.length
        val idEnd = jsonArray.indexOf('"', idStart)
        return jsonArray.substring(idStart, idEnd)
    }

    private fun classpathBytes(path: String): ByteArray {
        LedgerApiIntegrationTest::class.java.getResourceAsStream(path)!!.use { stream ->
            return stream.readAllBytes()
        }
    }
}