package com.example.cashCombine.ledger.imports

data class ImportResult(
    @get:JvmName("id") val id: ImportBatchId,
    @get:JvmName("accepted") val accepted: Int,
    @get:JvmName("duplicate") val duplicate: Int,
    @get:JvmName("rejected") val rejected: Int)