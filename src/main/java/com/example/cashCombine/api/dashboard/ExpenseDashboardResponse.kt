package com.example.cashCombine.api.dashboard

import com.example.cashCombine.ledger.dashboard.ExpenseDashboard
import java.math.BigDecimal

data class ExpenseDashboardResponse(
    val totalExpenses: BigDecimal,
    val expenseTransactionCount: Int,
    val categories: List<CategorySpendResponse>,
    val totalIncome: BigDecimal,
    val incomeTransactionCount: Int,
    val incomeCategories: List<CategorySpendResponse>,
) {

    companion object {
        fun from(dashboard: ExpenseDashboard): ExpenseDashboardResponse =
            ExpenseDashboardResponse(
                dashboard.totalExpenses,
                dashboard.expenseTransactionCount,
                dashboard.categories.map { CategorySpendResponse.from(it) },
                dashboard.totalIncome,
                dashboard.incomeTransactionCount,
                dashboard.incomeCategories.map { CategorySpendResponse.from(it) },
            )
    }
}