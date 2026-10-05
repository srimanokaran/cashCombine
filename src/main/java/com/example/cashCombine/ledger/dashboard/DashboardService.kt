package com.example.cashCombine.ledger.dashboard

import com.example.cashCombine.ledger.accounts.AccountId
import com.example.cashCombine.ledger.accounts.AccountRepository
import com.example.cashCombine.ledger.categorisation.Category
import com.example.cashCombine.ledger.categorisation.CategoryId
import com.example.cashCombine.ledger.categorisation.CategoryNotFoundException
import com.example.cashCombine.ledger.categorisation.CategoryRepository
import com.example.cashCombine.ledger.transactions.Transaction
import com.example.cashCombine.ledger.transactions.TransactionRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.util.TreeMap
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
open class DashboardService(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) {

    fun expenseBreakdown(): ExpenseDashboard {
        return expenseBreakdown(DateWindow.ALL)
    }

    fun expenseBreakdown(window: DateWindow): ExpenseDashboard {
        val categoriesById = categoriesById()
        val expenseNets = mutableMapOf<CategoryId, BigDecimal>()
        val expenseCounts = mutableMapOf<CategoryId, Int>()
        val incomeTotals = mutableMapOf<CategoryId, BigDecimal>()
        val incomeCounts = mutableMapOf<CategoryId, Int>()

        for (transaction in transactionRepository.findAll()) {
            if (!window.contains(transaction.date())) {
                continue
            }
            classify(transaction, categoriesById, expenseNets, expenseCounts, incomeTotals, incomeCounts)
        }

        val expenses = toExpenseBreakdown(categoriesById, expenseNets, expenseCounts)
        val income = toIncomeBreakdown(categoriesById, incomeTotals, incomeCounts)
        return ExpenseDashboard(
            expenses.total,
            expenses.count,
            expenses.categories,
            income.total,
            income.count,
            income.categories)
    }

    fun monthlyCashflow(): List<MonthlyCashflow> {
        val categoriesById = categoriesById()
        val buckets = TreeMap<YearMonth, MonthBucket>()

        for (transaction in transactionRepository.findAll()) {
            val month = YearMonth.from(transaction.date())
            val bucket = buckets.computeIfAbsent(month) { MonthBucket() }
            val category = categoriesById[transaction.categoryId()]
            if (category == null || category.isExcludedFromExpenses) {
                continue
            }

            if (category.isIncomeCreditCategory && transaction.amount().signum() > 0) {
                bucket.income = bucket.income.add(transaction.amount())
                bucket.incomeCount++
                continue
            }

            if (category.isIncome) {
                continue
            }

            bucket.expenseNet = bucket.expenseNet.add(transaction.amount())
            bucket.expenseCount++
        }

        if (buckets.isEmpty()) {
            return listOf()
        }

        var cursor = buckets.firstKey()
        val last = buckets.lastKey()
        val rows = mutableListOf<MonthlyCashflow>()
        while (!cursor.isAfter(last)) {
            val bucket = buckets.getOrDefault(cursor, MonthBucket())
            val expenses = bucket.expenseNet.negate().max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
            val income = bucket.income.setScale(2, RoundingMode.HALF_UP)
            val net = income.subtract(expenses).setScale(2, RoundingMode.HALF_UP)
            rows.add(MonthlyCashflow(
                cursor, expenses, income, net, bucket.expenseCount, bucket.incomeCount))
            cursor = cursor.plusMonths(1)
        }
        return rows.toList()
    }

    fun expenseTransactions(categoryId: CategoryId): List<ExpenseTransaction> {
        return expenseTransactions(categoryId, DateWindow.ALL)
    }

    fun expenseTransactions(categoryId: CategoryId, window: DateWindow): List<ExpenseTransaction> {
        val category = categoryRepository.findById(categoryId)
            ?: throw CategoryNotFoundException(categoryId)
        if (category.isExcludedFromExpenses || category.isIncome) {
            return listOf()
        }
        if (category.isUncategorised) {
            return listTransactions(categoryId, ListMode.EXPENSE_ONLY, window)
        }
        return listTransactions(categoryId, ListMode.ALL, window)
    }

    fun incomeTransactions(categoryId: CategoryId): List<ExpenseTransaction> {
        return incomeTransactions(categoryId, DateWindow.ALL)
    }

    fun incomeTransactions(categoryId: CategoryId, window: DateWindow): List<ExpenseTransaction> {
        val category = categoryRepository.findById(categoryId)
            ?: throw CategoryNotFoundException(categoryId)
        if (!category.isIncomeCreditCategory) {
            return listOf()
        }
        return listTransactions(categoryId, ListMode.INCOME_ONLY, window)
    }

    private fun classify(
        transaction: Transaction,
        categoriesById: Map<CategoryId, Category>,
        expenseNets: MutableMap<CategoryId, BigDecimal>,
        expenseCounts: MutableMap<CategoryId, Int>,
        incomeTotals: MutableMap<CategoryId, BigDecimal>,
        incomeCounts: MutableMap<CategoryId, Int>
    ) {
        val category = categoriesById[transaction.categoryId()]
        if (category == null || category.isExcludedFromExpenses) {
            return
        }

        if (category.isIncomeCreditCategory && transaction.amount().signum() > 0) {
            incomeTotals.merge(category.id, transaction.amount()) { a, b -> a.add(b) }
            incomeCounts.merge(category.id, 1) { a, b -> a + b }
            return
        }

        if (category.isIncome) {
            return
        }

        expenseNets.merge(category.id, transaction.amount()) { a, b -> a.add(b) }
        expenseCounts.merge(category.id, 1) { a, b -> a + b }
    }

    private enum class ListMode {
        ALL,
        INCOME_ONLY,
        EXPENSE_ONLY
    }

    private fun listTransactions(
        categoryId: CategoryId, mode: ListMode, window: DateWindow
    ): List<ExpenseTransaction> {
        val accountNames = mutableMapOf<AccountId, String>()
        for (account in accountRepository.findAll()) {
            accountNames[account.id()] = account.name()
        }

        val rows = mutableListOf<ExpenseTransaction>()
        for (transaction in transactionRepository.findAll()) {
            if (transaction.categoryId() != categoryId) {
                continue
            }
            if (!window.contains(transaction.date())) {
                continue
            }
            val sign = transaction.amount().signum()
            if (mode == ListMode.INCOME_ONLY && sign <= 0) {
                continue
            }
            if (mode == ListMode.EXPENSE_ONLY && sign >= 0) {
                continue
            }
            rows.add(ExpenseTransaction(
                transaction.id(),
                transaction.accountId(),
                accountNames.getOrDefault(transaction.accountId(), "Unknown"),
                transaction.date(),
                transaction.amount().setScale(2, RoundingMode.HALF_UP),
                transaction.description()))
        }

        rows.sortWith(compareByDescending<ExpenseTransaction> { it.date }
            .thenBy { it.description })
        return rows.toList()
    }

    private fun toExpenseBreakdown(
        categoriesById: Map<CategoryId, Category>,
        nets: Map<CategoryId, BigDecimal>,
        counts: Map<CategoryId, Int>
    ): Breakdown {
        val categories = mutableListOf<CategorySpend>()
        var signedTotal = BigDecimal.ZERO
        var positiveTotal = BigDecimal.ZERO
        var count = 0

        for ((categoryId, value) in nets) {
            val categoryCount = counts.getOrDefault(categoryId, 0)
            if (categoryCount == 0) {
                continue
            }
            val netSpend = value.negate().setScale(2, RoundingMode.HALF_UP)
            val category = categoriesById[categoryId]
            categories.add(CategorySpend(
                categoryId,
                category?.name ?: "Unknown",
                netSpend,
                BigDecimal.ZERO,
                categoryCount))
            signedTotal = signedTotal.add(netSpend)
            if (netSpend.signum() > 0) {
                positiveTotal = positiveTotal.add(netSpend)
            }
            count += categoryCount
        }

        for (i in categories.indices) {
            val row = categories[i]
            var percent = BigDecimal.ZERO
            if (positiveTotal.signum() > 0 && row.amount.signum() > 0) {
                percent = row.amount
                    .multiply(BigDecimal.valueOf(100))
                    .divide(positiveTotal, 1, RoundingMode.HALF_UP)
            }
            categories[i] = CategorySpend(
                row.categoryId, row.categoryName, row.amount, percent, row.transactionCount)
        }

        categories.sortWith(compareByDescending<CategorySpend> { it.amount }
            .thenBy { it.categoryName })
        val total = signedTotal.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
        return Breakdown(total, count, categories.toList())
    }

    private fun toIncomeBreakdown(
        categoriesById: Map<CategoryId, Category>,
        totals: Map<CategoryId, BigDecimal>,
        counts: Map<CategoryId, Int>
    ): Breakdown {
        val categories = mutableListOf<CategorySpend>()
        var total = BigDecimal.ZERO
        var count = 0

        for ((categoryId, value) in totals) {
            val amount = value.setScale(2, RoundingMode.HALF_UP)
            if (amount.signum() <= 0) {
                continue
            }
            val category = categoriesById[categoryId]
            val categoryCount = counts.getOrDefault(categoryId, 0)
            categories.add(CategorySpend(
                categoryId,
                category?.name ?: "Unknown",
                amount,
                BigDecimal.ZERO,
                categoryCount))
            total = total.add(amount)
            count += categoryCount
        }

        for (i in categories.indices) {
            val row = categories[i]
            val percent = if (total.signum() == 0)
                BigDecimal.ZERO
            else
                row.amount.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP)
            categories[i] = CategorySpend(
                row.categoryId, row.categoryName, row.amount, percent, row.transactionCount)
        }

        categories.sortByDescending { it.amount }
        return Breakdown(total.setScale(2, RoundingMode.HALF_UP), count, categories.toList())
    }

    private fun categoriesById(): Map<CategoryId, Category> {
        val categoriesById = mutableMapOf<CategoryId, Category>()
        for (category in categoryRepository.findAll()) {
            categoriesById[category.id] = category
        }
        return categoriesById
    }

    private data class Breakdown(val total: BigDecimal, val count: Int, val categories: List<CategorySpend>)

    private class MonthBucket {
        var expenseNet: BigDecimal = BigDecimal.ZERO
        var income: BigDecimal = BigDecimal.ZERO
        var expenseCount: Int = 0
        var incomeCount: Int = 0
    }
}