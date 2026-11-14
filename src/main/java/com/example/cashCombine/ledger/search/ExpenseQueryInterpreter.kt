package com.example.cashCombine.ledger.search

/**
 * Turns a free-text query into related search terms (synonyms, cities, merchant fragments).
 * It never sees transactions and never computes a total: Kotlin always re-runs the same
 * deterministic search with whatever terms come back.
 */
interface ExpenseQueryInterpreter {

    /** Related terms for [query], or null when no interpretation is available. */
    fun expand(query: String): List<String>?
}

/** Used when the interpreter is disabled or not configured. */
class NoopExpenseQueryInterpreter : ExpenseQueryInterpreter {

    override fun expand(query: String): List<String>? = null
}
