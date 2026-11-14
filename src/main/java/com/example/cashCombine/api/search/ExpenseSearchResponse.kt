package com.example.cashCombine.api.search

import com.example.cashCombine.ledger.search.ExpenseSearchResult
import java.math.BigDecimal
import java.time.LocalDate

data class ExpenseSearchResponse(
    val query: String,
    val totalSpend: BigDecimal,
    val expenseCount: Int,
    val excludedCount: Int,
    val expandedTerms: List<String>,
    val firstDate: LocalDate?,
    val lastDate: LocalDate?,
    val categories: List<SearchCategorySplitResponse>,
    val suggestions: List<SearchSuggestionResponse>,
    val transactions: List<SearchTransactionResponse>,
    val truncated: Boolean,
) {

    companion object {
        fun from(result: ExpenseSearchResult): ExpenseSearchResponse =
            ExpenseSearchResponse(
                result.query,
                result.totalSpend,
                result.expenseCount,
                result.excludedCount,
                result.expandedTerms,
                result.firstDate,
                result.lastDate,
                result.categories.map { SearchCategorySplitResponse.from(it) },
                result.suggestions.map { SearchSuggestionResponse.from(it) },
                result.transactions.map { SearchTransactionResponse.from(it) },
                result.truncated,
            )
    }
}
