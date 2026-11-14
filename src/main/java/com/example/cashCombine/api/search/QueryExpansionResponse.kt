package com.example.cashCombine.api.search

data class QueryExpansionResponse(
    val available: Boolean,
    val query: String,
    val terms: List<String>,
)
