package com.example.cashCombine.ledger.dashboard

import com.example.cashCombine.ledger.accounts.Account
import com.example.cashCombine.ledger.accounts.AccountId
import com.example.cashCombine.ledger.accounts.AccountType
import com.example.cashCombine.ledger.accounts.InMemoryAccountRepository
import com.example.cashCombine.ledger.categorisation.Category
import com.example.cashCombine.ledger.categorisation.InMemoryCategoryRepository
import com.example.cashCombine.ledger.imports.ParsedTransactionRow
import com.example.cashCombine.ledger.transactions.InMemoryTransactionRepository
import com.example.cashCombine.ledger.transactions.Transaction
import org.assertj.core.api.Assertions.assertThat
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DashboardServiceTest {

    private lateinit var categoryRepository: InMemoryCategoryRepository
    private lateinit var transactionRepository: InMemoryTransactionRepository
    private lateinit var accountRepository: InMemoryAccountRepository
    private lateinit var dashboardService: DashboardService
    private lateinit var groceries: Category
    private lateinit var dining: Category
    private lateinit var fundsBetweenAccounts: Category
    private lateinit var uncategorised: Category
    private lateinit var income: Category
    private lateinit var everyday: Account

    @BeforeEach
    fun setUp() {
        categoryRepository = InMemoryCategoryRepository()
        transactionRepository = InMemoryTransactionRepository()
        accountRepository = InMemoryAccountRepository()
        uncategorised = categoryRepository.save(Category.uncategorised())
        groceries = categoryRepository.save(Category.create("Groceries"))
        dining = categoryRepository.save(Category.create("Dining"))
        income = categoryRepository.save(Category.create(Category.INCOME_NAME))
        fundsBetweenAccounts = categoryRepository.save(Category.create(Category.FUNDS_BETWEEN_ACCOUNTS_NAME))
        everyday = accountRepository.save(Account.create("Everyday", AccountType.COMMBANK))
        dashboardService = DashboardService(transactionRepository, categoryRepository, accountRepository)
    }

    @Test
    fun `aggregates negative amounts by category`() {
        val accountId = everyday.id()
        save(accountId, "-40.00", groceries, "WOOLWORTHS A", LocalDate.of(2026, 7, 10))
        save(accountId, "-10.00", groceries, "WOOLWORTHS B", LocalDate.of(2026, 7, 9))
        save(accountId, "-25.00", dining, "CAFE", LocalDate.of(2026, 7, 8))
        save(accountId, "+100.00", uncategorised, "FRIEND PAYBACK", LocalDate.of(2026, 7, 7))
        save(accountId, "-4472.00", fundsBetweenAccounts, "SAVINGS", LocalDate.of(2026, 7, 6))

        val dashboard = dashboardService.expenseBreakdown()

        assertThat(dashboard.totalExpenses).isEqualByComparingTo("75.00")
        assertThat(dashboard.expenseTransactionCount).isEqualTo(3)
        assertThat(dashboard.categories).hasSize(2)
        assertThat(dashboard.categories[0].categoryName).isEqualTo("Groceries")
        assertThat(dashboard.categories[0].amount).isEqualByComparingTo("50.00")
        assertThat(dashboard.categories[0].percent).isEqualByComparingTo("66.7")
        assertThat(dashboard.categories[1].categoryName).isEqualTo("Dining")
        assertThat(dashboard.categories[1].amount).isEqualByComparingTo("25.00")
        assertThat(dashboard.totalIncome).isEqualByComparingTo("100.00")
        assertThat(dashboard.incomeCategories).hasSize(1)
        assertThat(dashboard.incomeCategories[0].categoryName).isEqualTo("Uncategorised")
    }

    @Test
    fun `income and uncategorised credits count as income while named credits offset expenses`() {
        val accountId = everyday.id()
        save(accountId, "+3200.00", income, "PAYROLL", LocalDate.of(2026, 7, 10))
        save(accountId, "-80.00", dining, "DINNER WITH FRIENDS", LocalDate.of(2026, 7, 9))
        save(accountId, "+30.00", dining, "FRIEND PAYBACK", LocalDate.of(2026, 7, 8))
        save(accountId, "+50.00", uncategorised, "RANDOM CREDIT", LocalDate.of(2026, 7, 7))
        save(accountId, "+4472.00", fundsBetweenAccounts, "FROM SAVINGS", LocalDate.of(2026, 7, 6))
        save(accountId, "-20.00", groceries, "WOOLWORTHS", LocalDate.of(2026, 7, 5))

        val dashboard = dashboardService.expenseBreakdown()

        assertThat(dashboard.totalIncome).isEqualByComparingTo("3250.00")
        assertThat(dashboard.incomeTransactionCount).isEqualTo(2)
        assertThat(dashboard.incomeCategories).hasSize(2)
        assertThat(dashboard.incomeCategories[0].categoryName).isEqualTo("Income")
        assertThat(dashboard.incomeCategories[0].amount).isEqualByComparingTo("3200.00")
        assertThat(dashboard.incomeCategories[1].categoryName).isEqualTo("Uncategorised")
        assertThat(dashboard.incomeCategories[1].amount).isEqualByComparingTo("50.00")

        assertThat(dashboard.totalExpenses).isEqualByComparingTo("70.00")
        assertThat(dashboard.categories).hasSize(2)
        assertThat(dashboard.categories[0].categoryName).isEqualTo("Dining")
        assertThat(dashboard.categories[0].amount).isEqualByComparingTo("50.00")
        assertThat(dashboard.categories[1].categoryName).isEqualTo("Groceries")
        assertThat(dashboard.categories[1].amount).isEqualByComparingTo("20.00")
    }

    @Test
    fun `shows category profit when credits exceed spend`() {
        val accountId = everyday.id()
        val entertainment = categoryRepository.save(Category.create("Entertainment"))
        save(accountId, "-15.26", entertainment, "MOVIE", LocalDate.of(2026, 7, 10))
        save(accountId, "+42.76", entertainment, "FRIEND PAYBACK", LocalDate.of(2026, 7, 11))
        save(accountId, "-50.00", groceries, "WOOLWORTHS", LocalDate.of(2026, 7, 9))

        val dashboard = dashboardService.expenseBreakdown()

        assertThat(dashboard.categories).hasSize(2)
        assertThat(dashboard.categories[0].categoryName).isEqualTo("Groceries")
        assertThat(dashboard.categories[0].amount).isEqualByComparingTo("50.00")
        assertThat(dashboard.categories[1].categoryName).isEqualTo("Entertainment")
        assertThat(dashboard.categories[1].amount).isEqualByComparingTo("-27.50")
        assertThat(dashboard.totalExpenses).isEqualByComparingTo("22.50")
    }

    @Test
    fun `lists expense transactions including reimbursements newest first`() {
        val accountId = everyday.id()
        save(accountId, "-40.00", groceries, "WOOLWORTHS A", LocalDate.of(2026, 7, 10))
        save(accountId, "-10.00", groceries, "WOOLWORTHS B", LocalDate.of(2026, 7, 11))
        save(accountId, "+5.00", groceries, "REFUND", LocalDate.of(2026, 7, 12))
        save(accountId, "-25.00", dining, "CAFE", LocalDate.of(2026, 7, 8))

        val rows = dashboardService.expenseTransactions(groceries.id)

        assertThat(rows).hasSize(3)
        assertThat(rows[0].description).isEqualTo("REFUND")
        assertThat(rows[0].amount).isEqualByComparingTo("5.00")
        assertThat(rows[1].description).isEqualTo("WOOLWORTHS B")
        assertThat(rows[1].amount).isEqualByComparingTo("-10.00")
        assertThat(rows[2].description).isEqualTo("WOOLWORTHS A")
        assertThat(rows[2].amount).isEqualByComparingTo("-40.00")
    }

    @Test
    fun `lists income transactions for uncategorised credits`() {
        val accountId = everyday.id()
        save(accountId, "+50.00", uncategorised, "FRIEND PAYBACK", LocalDate.of(2026, 7, 12))
        save(accountId, "-20.00", uncategorised, "UNKNOWN SPEND", LocalDate.of(2026, 7, 11))

        val incomeRows = dashboardService.incomeTransactions(uncategorised.id)
        val expenseRows = dashboardService.expenseTransactions(uncategorised.id)

        assertThat(incomeRows).hasSize(1)
        assertThat(incomeRows[0].description).isEqualTo("FRIEND PAYBACK")
        assertThat(expenseRows).hasSize(1)
        assertThat(expenseRows[0].description).isEqualTo("UNKNOWN SPEND")
    }

    @Test
    fun `lists income transactions for category newest first`() {
        val accountId = everyday.id()
        save(accountId, "+3200.00", income, "PAYROLL A", LocalDate.of(2026, 7, 10))
        save(accountId, "+100.00", income, "PAYROLL B", LocalDate.of(2026, 7, 12))
        save(accountId, "-50.00", income, "CORRECTION", LocalDate.of(2026, 7, 11))

        val rows = dashboardService.incomeTransactions(income.id)

        assertThat(rows).hasSize(2)
        assertThat(rows[0].description).isEqualTo("PAYROLL B")
        assertThat(rows[0].amount).isEqualByComparingTo("100.00")
        assertThat(rows[1].description).isEqualTo("PAYROLL A")
    }

    @Test
    fun `empty ledger returns zero`() {
        val dashboard = dashboardService.expenseBreakdown()

        assertThat(dashboard.totalExpenses).isEqualByComparingTo("0.00")
        assertThat(dashboard.expenseTransactionCount).isZero()
        assertThat(dashboard.categories).isEmpty()
        assertThat(dashboard.totalIncome).isEqualByComparingTo("0.00")
        assertThat(dashboard.incomeTransactionCount).isZero()
        assertThat(dashboard.incomeCategories).isEmpty()
    }

    @Test
    fun `card merchants count toward expenses while cash card payments are excluded`() {
        val funds = fundsBetweenAccounts
        val card = accountRepository.save(Account.create("Qantas Money", AccountType.NAB_CREDIT_CARD))

        save(everyday.id(), "-6000.00", funds, "Qantas Credit Cards BPAY", LocalDate.of(2026, 7, 15))
        save(card.id(), "-45.00", groceries, "WOOLWORTHS 1234", LocalDate.of(2026, 7, 10))
        save(card.id(), "-25.00", dining, "CAFE", LocalDate.of(2026, 7, 9))
        save(everyday.id(), "-20.00", groceries, "COLES", LocalDate.of(2026, 7, 8))

        val dashboard = dashboardService.expenseBreakdown()

        assertThat(dashboard.totalExpenses).isEqualByComparingTo("90.00")
        assertThat(dashboard.expenseTransactionCount).isEqualTo(3)
        assertThat(dashboard.categories.map { it.categoryName })
            .containsExactly("Groceries", "Dining")
        assertThat(dashboard.categories[0].amount).isEqualByComparingTo("65.00")
        assertThat(dashboard.categories[1].amount).isEqualByComparingTo("25.00")

        assertThat(dashboardService.expenseTransactions(groceries.id)).hasSize(2)
        assertThat(dashboardService.expenseTransactions(dining.id)).hasSize(1)
    }

    @Test
    fun `monthly cashflow buckets by month and fills gaps`() {
        val accountId = everyday.id()
        save(accountId, "-40.00", groceries, "WOOLWORTHS MAY", LocalDate.of(2026, 5, 10))
        save(accountId, "+3200.00", income, "PAYROLL MAY", LocalDate.of(2026, 5, 15))
        save(accountId, "-25.00", dining, "CAFE JUL", LocalDate.of(2026, 7, 8))
        save(accountId, "+50.00", uncategorised, "CREDIT JUL", LocalDate.of(2026, 7, 9))
        save(accountId, "-6000.00", fundsBetweenAccounts, "CARD PAYMENT", LocalDate.of(2026, 7, 10))

        val months = dashboardService.monthlyCashflow()

        assertThat(months).hasSize(3)
        assertThat(months[0].month).hasToString("2026-05")
        assertThat(months[0].totalExpenses).isEqualByComparingTo("40.00")
        assertThat(months[0].totalIncome).isEqualByComparingTo("3200.00")
        assertThat(months[0].net).isEqualByComparingTo("3160.00")
        assertThat(months[0].expenseTransactionCount).isEqualTo(1)
        assertThat(months[0].incomeTransactionCount).isEqualTo(1)

        assertThat(months[1].month).hasToString("2026-06")
        assertThat(months[1].totalExpenses).isEqualByComparingTo("0.00")
        assertThat(months[1].totalIncome).isEqualByComparingTo("0.00")
        assertThat(months[1].net).isEqualByComparingTo("0.00")

        assertThat(months[2].month).hasToString("2026-07")
        assertThat(months[2].totalExpenses).isEqualByComparingTo("25.00")
        assertThat(months[2].totalIncome).isEqualByComparingTo("50.00")
        assertThat(months[2].net).isEqualByComparingTo("25.00")
    }

    @Test
    fun `expense breakdown respects date window`() {
        val accountId = everyday.id()
        save(accountId, "-40.00", groceries, "MAY SPEND", LocalDate.of(2026, 5, 10))
        save(accountId, "-25.00", dining, "JUL SPEND", LocalDate.of(2026, 7, 8))
        save(accountId, "+100.00", income, "JUL PAY", LocalDate.of(2026, 7, 9))

        val july = dashboardService.expenseBreakdown(
            DateWindow(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))

        assertThat(july.totalExpenses).isEqualByComparingTo("25.00")
        assertThat(july.categories.map { it.categoryName }).containsExactly("Dining")
        assertThat(july.totalIncome).isEqualByComparingTo("100.00")

        val julyGroceries = dashboardService.expenseTransactions(
            groceries.id, DateWindow(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
        val julyDining = dashboardService.expenseTransactions(
            dining.id, DateWindow(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))

        assertThat(julyGroceries).isEmpty()
        assertThat(julyDining).hasSize(1)
        assertThat(julyDining[0].description).isEqualTo("JUL SPEND")
    }

    private fun save(
        accountId: AccountId, amount: String, category: Category, description: String, date: LocalDate
    ) {
        transactionRepository.save(Transaction.create(
            accountId,
            ParsedTransactionRow(date, BigDecimal(amount), description, BigDecimal("100.00")),
            category.id))
    }
}