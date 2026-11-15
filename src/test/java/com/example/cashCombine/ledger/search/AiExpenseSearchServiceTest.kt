package com.example.cashCombine.ledger.search

import com.example.cashCombine.ledger.accounts.Account
import com.example.cashCombine.ledger.accounts.AccountType
import com.example.cashCombine.ledger.accounts.InMemoryAccountRepository
import com.example.cashCombine.ledger.categorisation.Category
import com.example.cashCombine.ledger.categorisation.InMemoryCategoryRepository
import com.example.cashCombine.ledger.imports.ParsedTransactionRow
import com.example.cashCombine.ledger.transactions.InMemoryTransactionRepository
import com.example.cashCombine.ledger.transactions.Transaction
import java.math.BigDecimal
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class AiExpenseSearchServiceTest {
    private val accounts = InMemoryAccountRepository()
    private val categories = InMemoryCategoryRepository()
    private val transactions = InMemoryTransactionRepository()
    private val account = accounts.save(Account.create("Private account", AccountType.COMMBANK))
    private val travel = categories.save(Category.create("Travel"))
    private val income = categories.save(Category.create(Category.INCOME_NAME))
    private val transfers = categories.save(Category.create(Category.FUNDS_BETWEEN_ACCOUNTS_NAME))
    private val runtime = Mockito.mock(LocalOllamaRuntime::class.java)
    private val deterministic = ExpenseSearchService(transactions, categories, accounts)

    private fun save(amount: String, category: Category, description: String) = transactions.save(
        Transaction.create(account.id(), ParsedTransactionRow(
            LocalDate.of(2026, 3, 2), BigDecimal(amount), description, BigDecimal("91234.56")), category.id)
    )

    @Test
    fun `selected expenses refunds and exclusions use shared aggregation`() {
        save("-100.00", travel, "HOTEL 123456789")
        save("20.00", travel, "HOTEL REFUND")
        save("500.00", income, "SALARY")
        save("-200.00", transfers, "TRANSFER")
        var seen = emptyList<AiSearchRow>()
        val selector = object : AiTransactionSelector {
            override fun select(query: String, rows: List<AiSearchRow>): Set<String> {
                seen = rows
                return rows.map { it.id }.toSet()
            }
        }
        val result = AiExpenseSearchService(transactions, categories, deterministic, selector, runtime)
            .search("all spending")

        Mockito.verify(runtime).ensureAvailable()
        assertThat(seen.first().description).isEqualTo("HOTEL [number redacted]")
        assertThat(seen.toString()).doesNotContain("91234.56", "Private account", "123456789")
        assertThat(result.expenseCount).isEqualTo(2)
        assertThat(result.excludedCount).isEqualTo(2)
        assertThat(result.totalSpend).isEqualByComparingTo("80.00")
        assertThat(result.categories.single().amount).isEqualByComparingTo("80.00")
        assertThat(result.categories.single().transactionCount).isEqualTo(2)
        assertThat(result.transactions.map { it.matchedBy }).containsOnly(SearchMatch.AI)
        assertThat(result.truncated).isFalse()
    }

    @Test
    fun `bad queries and hallucinations fail instead of partial results`() {
        save("-10.00", travel, "SHOP")
        val selector = object : AiTransactionSelector {
            override fun select(query: String, rows: List<AiSearchRow>) = setOf("not-a-transaction")
        }
        val service = AiExpenseSearchService(transactions, categories, deterministic, selector, runtime)
        for (query in listOf(" ", "x".repeat(501), "account 12345678")) {
            assertThatThrownBy { service.search(query) }.isInstanceOf(IllegalArgumentException::class.java)
        }
        Mockito.verifyNoInteractions(runtime)
        assertThatThrownBy { service.search("shops") }.isInstanceOf(AiSearchException::class.java)
    }

    @Test
    fun `work and result caps fail explicitly`() {
        val selector = object : AiTransactionSelector {
            override fun select(query: String, rows: List<AiSearchRow>) = rows.map { it.id }.toSet()
        }
        val service = AiExpenseSearchService(transactions, categories, deterministic, selector, runtime)
        repeat(501) { save("-1", travel, "SHOP $it") }
        assertThatThrownBy { service.search("shop") }.isInstanceOf(AiSearchException::class.java)
            .hasMessageContaining("too many transactions")
        repeat(500) { save("-1", travel, "SHOP extra $it") }
        Mockito.clearInvocations(runtime)
        assertThatThrownBy { service.search("shop") }.isInstanceOf(AiSearchException::class.java)
            .hasMessageContaining("Too many transactions")
        Mockito.verifyNoInteractions(runtime)
    }

    @Test
    fun `missing model reports a safe actionable error`() {
        Mockito.doThrow(LocalOllamaException(LocalOllamaException.Reason.MODEL_MISSING))
            .`when`(runtime).ensureAvailable()
        val selector = object : AiTransactionSelector {
            override fun select(query: String, rows: List<AiSearchRow>) = emptySet<String>()
        }
        val service = AiExpenseSearchService(transactions, categories, deterministic, selector, runtime)
        assertThatThrownBy { service.start() }.isInstanceOf(AiSearchException::class.java)
            .hasMessageContaining("ollama pull llama3")
    }

    @Test
    fun `runtime failures have generic errors`() {
        Mockito.doThrow(IllegalStateException("private detail")).`when`(runtime).ensureAvailable()
        val selector = object : AiTransactionSelector {
            override fun select(query: String, rows: List<AiSearchRow>) = emptySet<String>()
        }
        val service = AiExpenseSearchService(transactions, categories, deterministic, selector, runtime)
        assertThatThrownBy { service.start() }.isInstanceOf(AiSearchException::class.java)
            .hasMessageNotContaining("private detail")
    }
}
