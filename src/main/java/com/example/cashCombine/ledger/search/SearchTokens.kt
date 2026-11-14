package com.example.cashCombine.ledger.search

/**
 * Token rules shared by the user query and by anything a query interpreter suggests.
 * Keeping one sanitiser means an expanded term can never be more powerful than a typed one.
 */
object SearchTokens {

    const val MAX_TERMS = 8
    const val MAX_TERM_LENGTH = 24
    const val MIN_TERM_LENGTH = 2

    private val ALLOWED = Regex("^[a-z0-9 ]+$")
    private val WHITESPACE = Regex("\\s+")

    /** Splits a free-text query into lowercase terms. */
    @JvmStatic
    fun parse(query: String?): List<String> {
        if (query.isNullOrBlank()) {
            return emptyList()
        }
        return query.lowercase()
            .replace(WHITESPACE, " ")
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .distinct()
    }

    /** Trims, lowercases and drops anything unsafe or too long, keeping order and at most [MAX_TERMS]. */
    @JvmStatic
    fun sanitise(terms: List<String>?): List<String> {
        if (terms.isNullOrEmpty()) {
            return emptyList()
        }
        val result = LinkedHashSet<String>()
        for (term in terms) {
            val clean = term.lowercase().replace(WHITESPACE, " ").trim()
            if (clean.length < MIN_TERM_LENGTH || clean.length > MAX_TERM_LENGTH) {
                continue
            }
            if (!ALLOWED.matches(clean)) {
                continue
            }
            result.add(clean)
            if (result.size >= MAX_TERMS) {
                break
            }
        }
        return result.toList()
    }
}
