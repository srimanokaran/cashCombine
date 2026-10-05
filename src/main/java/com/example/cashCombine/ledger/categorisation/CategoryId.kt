package com.example.cashCombine.ledger.categorisation

import java.util.UUID

data class CategoryId(@get:JvmName("value") val value: UUID) {
    init {
        require(value != null) { "Category id is required" }
    }

    companion object {
        fun generate(): CategoryId = CategoryId(UUID.randomUUID())
    }
}