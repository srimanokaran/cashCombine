package com.example.cashCombine.ledger.categorisation

class TransactionClassifier(
    private val ruleRepository: ClassificationRuleRepository,
    private val uncategorisedId: CategoryId
) {
    init {
        require(uncategorisedId != null) { "Uncategorised category id is required" }
    }

    fun classify(description: String?): CategoryId {
        return ruleRepository.findAll()
            .sortedByDescending { it.pattern.length }
            .filter { it.matches(description) }
            .map { it.categoryId }
            .firstOrNull()
            ?: uncategorisedId
    }
}