package com.example.cashCombine.api.search

import com.example.cashCombine.ledger.categorisation.CategoryAssignmentSource
import com.example.cashCombine.ledger.search.SearchMatch
import com.example.cashCombine.ledger.search.SearchTransaction
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class SearchTransactionResponse(
    val id: UUID,
    val accountId: UUID,
    val accountName: String,
    val date: LocalDate,
    val amount: BigDecimal,
    val description: String,
    val categoryId: UUID,
    val categoryName: String,
    val categoryAssignmentSource: CategoryAssignmentSource,
    val matchedBy: SearchMatch,
) {

    companion object {
        fun from(transaction: SearchTransaction): SearchTransactionResponse =
            SearchTransactionResponse(
                transaction.id.value,
                transaction.accountId.value,
                transaction.accountName,
                transaction.date,
                transaction.amount,
                transaction.description,
                transaction.categoryId.value,
                transaction.categoryName,
                transaction.categoryAssignmentSource,
                transaction.matchedBy,
            )
    }
}
