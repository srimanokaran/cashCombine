package com.example.cashCombine.ledger.search

import com.example.cashCombine.ledger.accounts.Account
import com.example.cashCombine.ledger.accounts.AccountId
import com.example.cashCombine.ledger.accounts.AccountType
import com.example.cashCombine.ledger.accounts.InMemoryAccountRepository
import com.example.cashCombine.ledger.categorisation.Category
import com.example.cashCombine.ledger.categorisation.InMemoryCategoryRepository
import com.example.cashCombine.ledger.dashboard.DateWindow
import com.example.cashCombine.ledger.imports.ParsedTransactionRow
import com.example.cashCombine.ledger.transactions.InMemoryTransactionRepository
import com.example.cashCombine.ledger.transactions.Transaction
import java.math.BigDecimal
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExpenseSearchServiceTest {

    private lateinit var categoryRepository: InMemoryCategoryRepository
    private lateinit var transactionRepository: InMemoryTransactionRepository
    private lateinit var accountRepository: InMemoryAccountRepository
    private lateinit var travel: Category
    private lateinit var groceries: Category
    private lateinit var uncategorised: Category
    private lateinit var income: Category
    private lateinit var fundsBetweenAccounts: Category
    private lateinit var everyday: Account
    private var interpreterTerms: List<String>? = null
    private var interpreterCalls = 0
    private lateinit var service: ExpenseSearchService

    @BeforeEach
    fun setUp() {
        categoryRepository = InMemoryCategoryRepository()
        transactionRepository = InMemoryTransactionRepository()
        accountRepository = InMemoryAccountRepository()
        uncategorised = categoryRepository.save(Category.uncategorised())
        groceries = categoryRepository.save(Category.create("Groceries"))
        travel = categoryRepository.save(Category.create("Japan trip"))
        income = categoryRepository.save(Category.create(Category.INCOME_NAME))
        fundsBetweenAccounts = categoryRepository.save(
            Category.create(Category.FUNDS_BETWEEN_ACCOUNTS_NAME))
        everyday = accountRepository.save(Account.create("Everyday", AccountType.COMMBANK))
        interpreterTerms = null
        interpreterCalls = 0
        service = ExpenseSearchService(
            transactionRepository, categoryRepository, accountRepository, FakeInterpreter())
    }

    @Test
    fun `matches every query term against description or category name`() {
        save("-920.00", travel, "JAPAN RAIL PASS", LocalDate.of(2026, 3, 2))
        save("-45.00", groceries, "WOOLWORTHS JAPANESE SNACKS", LocalDate.of(2026, 3, 3))
        save("-30.00", groceries, "WOOLWORTHS", LocalDate.of(2026, 3, 4))

        val result = service.search("japan rail")

        assertThat(result.expenseCount).isEqualTo(1)
        assertThat(result.transactions.single().description).isEqualTo("JAPAN RAIL PASS")
    }

    @Test
    fun `searching a category name returns everything in that category`() {
        save("-920.00", travel, "JAPAN RAIL PASS", LocalDate.of(2026, 3, 2))
        save("-210.50", travel, "SHIBUYA HOTEL", LocalDate.of(2026, 3, 5))
        save("-30.00", groceries, "WOOLWORTHS", LocalDate.of(2026, 3, 4))

        val result = service.search("japan trip")

        assertThat(result.expenseCount).isEqualTo(2)
        assertThat(result.totalSpend).isEqualByComparingTo("1130.50")
        assertThat(result.firstDate).isEqualTo(LocalDate.of(2026, 3, 2))
        assertThat(result.lastDate).isEqualTo(LocalDate.of(2026, 3, 5))
    }

    @Test
    fun `refunds reduce the total and a net refund stays negative`() {
        save("-920.00", travel, "JAPAN RAIL PASS", LocalDate.of(2026, 3, 2))
        save("+100.00", travel, "JAPAN RAIL PASS REFUND", LocalDate.of(2026, 3, 9))

        val result = service.search("japan rail pass")

        assertThat(result.expenseCount).isEqualTo(2)
        assertThat(result.totalSpend).isEqualByComparingTo("820.00")

        val refundOnly = service.search("japan rail pass refund")
        assertThat(refundOnly.totalSpend).isEqualByComparingTo("-100.00")
    }

    @Test
    fun `income and funds between accounts are counted but never totalled`() {
        save("-920.00", travel, "JAPAN RAIL PASS", LocalDate.of(2026, 3, 2))
        save("+3200.00", income, "JAPAN SALARY", LocalDate.of(2026, 3, 1))
        save("-6000.00", fundsBetweenAccounts, "JAPAN CARD PAYMENT", LocalDate.of(2026, 3, 6))

        val result = service.search("japan")

        assertThat(result.expenseCount).isEqualTo(1)
        assertThat(result.excludedCount).isEqualTo(2)
        assertThat(result.totalSpend).isEqualByComparingTo("920.00")
    }

    @Test
    fun `suggestions group merchants by normalised description and sort by spend`() {
        save("-920.00", travel, "JAPAN RAIL PASS 1234", LocalDate.of(2026, 3, 2))
        save("-15.00", travel, "JAPAN RAIL PASS 5678", LocalDate.of(2026, 3, 3))
        save("-120.00", travel, "NARITA EXPRESS TOKYO", LocalDate.of(2026, 3, 4))

        val result = service.search("japan")

        assertThat(result.suggestions).hasSize(2)
        assertThat(result.suggestions[0].label).isEqualTo("JAPAN RAIL PASS")
        assertThat(result.suggestions[0].matchCount).isEqualTo(2)
        assertThat(result.suggestions[0].total).isEqualByComparingTo("935.00")
        assertThat(result.suggestions[0].categoryName).isEqualTo("Japan trip")
        assertThat(result.suggestions[1].label).isEqualTo("NARITA EXPRESS TOKYO")
    }

    @Test
    fun `category splits carry net spend and counts`() {
        save("-920.00", travel, "JAPAN RAIL PASS", LocalDate.of(2026, 3, 2))
        save("+20.00", travel, "JAPAN RAIL PASS REFUND", LocalDate.of(2026, 3, 3))
        save("-100.00", groceries, "JAPANESE SNACKS", LocalDate.of(2026, 3, 4))

        val result = service.search("japan")

        assertThat(result.categories).hasSize(2)
        assertThat(result.categories[0].categoryName).isEqualTo("Japan trip")
        assertThat(result.categories[0].amount).isEqualByComparingTo("900.00")
        assertThat(result.categories[0].transactionCount).isEqualTo(2)
        assertThat(result.categories[1].amount).isEqualByComparingTo("100.00")
    }

    @Test
    fun `expanded terms match with OR and are marked as expanded`() {
        interpreterTerms = listOf("tokyo", "narita")
        save("-120.00", uncategorised, "NARITA EXPRESS TOKYO", LocalDate.of(2026, 3, 4))
        save("-45.00", groceries, "WOOLWORTHS", LocalDate.of(2026, 3, 5))

        val expansion = service.expand("japan")
        val result = service.search("japan", DateWindow.ALL, expansion.terms)

        assertThat(expansion.available).isTrue()
        assertThat(expansion.terms).containsExactly("tokyo", "narita")
        assertThat(result.expenseCount).isEqualTo(1)
        assertThat(result.transactions.single().matchedBy).isEqualTo(SearchMatch.EXPANDED)
    }

    @Test
    fun `expanded terms never override the typed query terms`() {
        // Typed terms are AND, expanded terms are OR: "japan rail" still requires both words.
        save("-920.00", travel, "JAPAN RAIL PASS", LocalDate.of(2026, 3, 2))
        save("-210.50", travel, "SHIBUYA HOTEL", LocalDate.of(2026, 3, 4))

        val result = service.search("japan rail", DateWindow.ALL, listOf("tokyo"))

        assertThat(result.expenseCount).isEqualTo(1)
        assertThat(result.transactions.single().description).isEqualTo("JAPAN RAIL PASS")
        assertThat(result.transactions.single().matchedBy).isEqualTo(SearchMatch.QUERY)
    }

    @Test
    fun `expand is unavailable when the interpreter is disabled`() {
        val disabled = ExpenseSearchService(
            transactionRepository, categoryRepository, accountRepository, NoopExpenseQueryInterpreter())

        val expansion = disabled.expand("japan")

        assertThat(expansion.available).isFalse()
        assertThat(expansion.terms).isEmpty()
    }

    @Test
    fun `expand skips short queries and sanitises interpreter output`() {
        interpreterTerms = listOf("Tokyo", "$12", "a", "narita")
        assertThat(service.expand("jp").available).isFalse()
        assertThat(interpreterCalls).isZero()

        val expansion = service.expand("japan")

        assertThat(expansion.terms).containsExactly("tokyo", "narita")
    }

    @Test
    fun `unsafe expand parameters are ignored`() {
        save("-920.00", travel, "JAPAN RAIL PASS", LocalDate.of(2026, 3, 2))

        val result = service.search("japan", DateWindow.ALL, listOf("'); DROP TABLE", "$12.00"))

        assertThat(result.expandedTerms).isEmpty()
        assertThat(result.expenseCount).isEqualTo(1)
    }

    @Test
    fun `blank query returns an empty result`() {
        save("-920.00", travel, "JAPAN RAIL PASS", LocalDate.of(2026, 3, 2))

        val result = service.search("   ")

        assertThat(result.expenseCount).isZero()
        assertThat(result.totalSpend).isEqualByComparingTo("0.00")
        assertThat(result.transactions).isEmpty()
        assertThat(result.categories).isEmpty()
        assertThat(result.suggestions).isEmpty()
        assertThat(result.firstDate).isNull()
    }

    @Test
    fun `respects the date window`() {
        save("-920.00", travel, "JAPAN RAIL PASS", LocalDate.of(2026, 3, 2))
        save("-210.50", travel, "SHIBUYA HOTEL", LocalDate.of(2026, 11, 5))

        val result = service.search(
            "japan trip",
            DateWindow(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)),
        )

        assertThat(result.expenseCount).isEqualTo(1)
        assertThat(result.totalSpend).isEqualByComparingTo("210.50")
        assertThat(result.transactions.single().description).isEqualTo("SHIBUYA HOTEL")
    }

    @Test
    fun `caps the transaction list but keeps counts and totals whole`() {
        repeat(5) { index ->
            save("-10.00", travel, "JAPAN RAIL PASS", LocalDate.of(2026, 3, index + 1))
        }

        val result = service.search("japan", DateWindow.ALL, emptyList(), limit = 3)

        assertThat(result.transactions).hasSize(3)
        assertThat(result.truncated).isTrue()
        assertThat(result.expenseCount).isEqualTo(5)
        assertThat(result.totalSpend).isEqualByComparingTo("50.00")
    }

    @Test
    fun `results are newest first`() {
        save("-10.00", travel, "JAPAN RAIL PASS", LocalDate.of(2026, 3, 1))
        save("-20.00", travel, "SHIBUYA HOTEL", LocalDate.of(2026, 3, 3))

        val result = service.search("japan trip")

        assertThat(result.transactions.map { it.date })
            .containsExactly(LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 1))
        assertThat(result.transactions[0].accountName).isEqualTo("Everyday")
        assertThat(result.transactions[0].categoryName).isEqualTo("Japan trip")
    }

    private fun save(amount: String, category: Category, description: String, date: LocalDate) {
        transactionRepository.save(Transaction.create(
            everyday.id(),
            ParsedTransactionRow(date, BigDecimal(amount), description, BigDecimal("100.00")),
            category.id))
    }

    private inner class FakeInterpreter : ExpenseQueryInterpreter {

        override fun expand(query: String): List<String>? {
            interpreterCalls++
            return interpreterTerms
        }
    }
}
