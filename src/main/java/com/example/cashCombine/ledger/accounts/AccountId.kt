package com.example.cashCombine.ledger.accounts

import java.util.UUID

data class AccountId(@get:JvmName("value") val value: UUID) {

    init {
        require(value.toString().isNotEmpty()) { "Account id is required" }
    }

    companion object {
        @JvmStatic
        fun generate() = AccountId(UUID.randomUUID())
    }
}