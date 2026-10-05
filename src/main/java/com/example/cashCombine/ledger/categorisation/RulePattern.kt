package com.example.cashCombine.ledger.categorisation

object RulePattern {

    @JvmStatic
    fun fromDescription(description: String?): String {
        require(!description.isNullOrBlank()) { "Classification pattern is required" }
        var pattern = description.trim()
        pattern = cutAtIgnoreCase(pattern, " Card xx")
        pattern = cutAtIgnoreCase(pattern, " Value Date:")
        val trimmed = pattern.trim()
        if (trimmed.isEmpty()) {
            return description.trim()
        }
        return trimmed
    }

    private fun cutAtIgnoreCase(value: String, marker: String): String {
        val index = indexOfIgnoreCase(value, marker)
        if (index < 0) {
            return value
        }
        return value.substring(0, index)
    }

    private fun indexOfIgnoreCase(value: String, marker: String): Int =
        value.lowercase().indexOf(marker.lowercase())
}