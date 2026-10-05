package com.example.cashCombine.ledger.imports

import java.util.UUID

data class ImportBatchId(@get:JvmName("value") val value: UUID) {
    init {
        require(value != null) { "Import batch id is required" }
    }

    companion object {
        fun generate(): ImportBatchId = ImportBatchId(UUID.randomUUID())
    }
}