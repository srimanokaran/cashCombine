package com.example.cashCombine.api.transactions

import com.example.cashCombine.ledger.categorisation.CategoryAssignmentSource
import com.example.cashCombine.ledger.transactions.Transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class TransactionResponse(
    val id: UUID,
    val accountId: UUID,
    val date: LocalDate,
    val amount: BigDecimal,
    val description: String,
    val balance: BigDecimal,
    val categoryId: UUID,
    val categoryAssignmentSource: CategoryAssignmentSource,
) {

    companion object {
        fun from(transaction: Transaction): TransactionResponse =
            TransactionResponse(
                transaction.id().value,
                transaction.accountId().value,
                transaction.date(),
                transaction.amount(),
                transaction.description(),
                transaction.balance(),
                transaction.categoryId().value,
                transaction.categoryAssignmentSource(),
            )
    }
}