package com.example.cashCombine.api.rules

import com.example.cashCombine.ledger.categorisation.ClassificationRule
import java.util.UUID

data class RuleResponse(val id: UUID, val pattern: String, val categoryId: UUID) {

    companion object {
        fun from(rule: ClassificationRule): RuleResponse =
            RuleResponse(rule.id.value, rule.pattern, rule.categoryId.value)
    }
}