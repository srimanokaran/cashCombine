package com.example.cashCombine.ledger.dashboard

import java.math.BigDecimal
import java.time.YearMonth

data class MonthlyCashflow(
    @get:JvmName("month") val month: YearMonth,
    @get:JvmName("totalExpenses") val totalExpenses: BigDecimal,
    @get:JvmName("totalIncome") val totalIncome: BigDecimal,
    @get:JvmName("net") val net: BigDecimal,
    @get:JvmName("expenseTransactionCount") val expenseTransactionCount: Int,
    @get:JvmName("incomeTransactionCount") val incomeTransactionCount: Int)