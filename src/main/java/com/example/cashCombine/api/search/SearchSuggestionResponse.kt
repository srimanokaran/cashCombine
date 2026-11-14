package com.example.cashCombine.api.search

import com.example.cashCombine.ledger.search.SearchSuggestion
import java.math.BigDecimal

data class SearchSuggestionResponse(
    val label: String,
    val matchCount: Int,
    val total: BigDecimal,
    val categoryName: String,
) {

    companion object {
        fun from(suggestion: SearchSuggestion): SearchSuggestionResponse =
            SearchSuggestionResponse(
                suggestion.label,
                suggestion.matchCount,
                suggestion.total,
                suggestion.categoryName,
            )
    }
}
