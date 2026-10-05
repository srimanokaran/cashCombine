package com.example.cashCombine.api.dashboard

import com.example.cashCombine.ledger.dashboard.ExpenseTransaction
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class ExpenseTransactionResponse(
    val id: UUID,
    val accountId: UUID,
    val accountName: String,
    val date: LocalDate,
    val amount: BigDecimal,
    val description: String,
) {

    companion object {
        fun from(transaction: ExpenseTransaction): ExpenseTransactionResponse =
            ExpenseTransactionResponse(
                transaction.id.value,
                transaction.accountId.value,
                transaction.accountName,
                transaction.date,
                transaction.amount,
                transaction.description,
            )
    }
}