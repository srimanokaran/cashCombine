package com.example.cashCombine.api.dashboard

import com.example.cashCombine.ledger.dashboard.MonthlyCashflow
import java.math.BigDecimal

data class MonthlyCashflowResponse(
    val month: String,
    val totalExpenses: BigDecimal,
    val totalIncome: BigDecimal,
    val net: BigDecimal,
    val expenseTransactionCount: Int,
    val incomeTransactionCount: Int,
) {

    companion object {
        fun from(row: MonthlyCashflow): MonthlyCashflowResponse =
            MonthlyCashflowResponse(
                row.month.toString(),
                row.totalExpenses,
                row.totalIncome,
                row.net,
                row.expenseTransactionCount,
                row.incomeTransactionCount,
            )
    }
}