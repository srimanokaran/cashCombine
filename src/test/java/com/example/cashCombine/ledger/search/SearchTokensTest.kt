package com.example.cashCombine.ledger.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SearchTokensTest {

    @Test
    fun `parses a free text query into distinct lowercase terms`() {
        assertThat(SearchTokens.parse("  Japan   Trip JAPAN ")).containsExactly("japan", "trip")
    }

    @Test
    fun `blank query has no terms`() {
        assertThat(SearchTokens.parse(null)).isEmpty()
        assertThat(SearchTokens.parse("   ")).isEmpty()
    }

    @Test
    fun `sanitise drops unsafe short long and duplicate terms`() {
        val terms = SearchTokens.sanitise(
            listOf("Tokyo", "  jr pass ", "a", "x".repeat(25), "'; DROP TABLE", "tokyo", "narita")
        )

        assertThat(terms).containsExactly("tokyo", "jr pass", "narita")
    }

    @Test
    fun `sanitise caps the number of terms`() {
        val terms = SearchTokens.sanitise((1..20).map { "term$it" })

        assertThat(terms).hasSize(SearchTokens.MAX_TERMS)
    }

    @Test
    fun `sanitise keeps numbers but rejects amounts and punctuation`() {
        assertThat(SearchTokens.sanitise(listOf("7 eleven", "$12.00", "qty-2")))
            .containsExactly("7 eleven")
    }
}
