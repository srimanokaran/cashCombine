package com.example.cashCombine.ledger.search

data class ExpenseQueryExpansion(
    @get:JvmName("available") val available: Boolean,
    @get:JvmName("query") val query: String,
    @get:JvmName("terms") val terms: List<String>)
