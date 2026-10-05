package com.example.cashCombine.ledger.transactions

import java.util.UUID

data class TransactionId(@get:JvmName("value") val value: UUID) {

    init {
        require(value.toString().isNotEmpty()) { "Transaction id is required" }
    }

    companion object {
        @JvmStatic
        fun generate() = TransactionId(UUID.randomUUID())
    }
}