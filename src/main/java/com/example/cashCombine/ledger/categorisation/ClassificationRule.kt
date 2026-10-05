package com.example.cashCombine.ledger.categorisation

import java.util.Locale

class ClassificationRule private constructor(
    @get:JvmName("id") val id: ClassificationRuleId,
    @get:JvmName("pattern") val pattern: String,
    @get:JvmName("categoryId") val categoryId: CategoryId
) {

    fun matches(description: String?): Boolean {
        if (description == null) return false
        return description.lowercase(Locale.ROOT).contains(pattern.lowercase(Locale.ROOT))
    }

    companion object {
        @JvmStatic
        fun create(pattern: String, categoryId: CategoryId): ClassificationRule {
            require(pattern.isNotBlank()) { "Classification pattern is required" }
            require(categoryId != null) { "Category id is required" }
            return ClassificationRule(ClassificationRuleId.generate(), pattern, categoryId)
        }

        @JvmStatic
        fun reconstitute(id: ClassificationRuleId, pattern: String, categoryId: CategoryId): ClassificationRule =
            ClassificationRule(id, pattern, categoryId)
    }
}