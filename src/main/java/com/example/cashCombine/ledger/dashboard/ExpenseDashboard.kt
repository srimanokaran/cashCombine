package com.example.cashCombine.ledger.dashboard

import java.math.BigDecimal

data class ExpenseDashboard(
    @get:JvmName("totalExpenses") val totalExpenses: BigDecimal,
    @get:JvmName("expenseTransactionCount") val expenseTransactionCount: Int,
    @get:JvmName("categories") val categories: List<CategorySpend>,
    @get:JvmName("totalIncome") val totalIncome: BigDecimal,
    @get:JvmName("incomeTransactionCount") val incomeTransactionCount: Int,
    @get:JvmName("incomeCategories") val incomeCategories: List<CategorySpend>)