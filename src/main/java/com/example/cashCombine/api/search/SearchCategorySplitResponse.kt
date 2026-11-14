package com.example.cashCombine.api.search

import com.example.cashCombine.ledger.search.SearchCategorySplit
import java.math.BigDecimal
import java.util.UUID

data class SearchCategorySplitResponse(
    val categoryId: UUID,
    val categoryName: String,
    val amount: BigDecimal,
    val transactionCount: Int,
) {

    companion object {
        fun from(split: SearchCategorySplit): SearchCategorySplitResponse =
            SearchCategorySplitResponse(
                split.categoryId.value,
                split.categoryName,
                split.amount,
                split.transactionCount,
            )
    }
}
