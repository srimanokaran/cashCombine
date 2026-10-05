package com.example.cashCombine.ledger.categorisation

import java.util.UUID

data class ClassificationRuleId(@get:JvmName("value") val value: UUID) {
    init {
        require(value != null) { "Classification rule id is required" }
    }

    companion object {
        fun generate(): ClassificationRuleId = ClassificationRuleId(UUID.randomUUID())
    }
}