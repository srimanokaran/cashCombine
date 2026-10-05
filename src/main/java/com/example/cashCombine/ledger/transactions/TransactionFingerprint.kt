package com.example.cashCombine.ledger.transactions

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.Objects

data class TransactionFingerprint(
    @get:JvmName("date") val date: LocalDate,
    @get:JvmName("description") val description: String,
    private val _amount: BigDecimal,
    private val _balance: BigDecimal,
) {

    @get:JvmName("amount")
    val amount: BigDecimal = canonicalMoney(_amount)

    @get:JvmName("balance")
    val balance: BigDecimal = canonicalMoney(_balance)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransactionFingerprint) return false
        return date == other.date && description == other.description &&
            amount == other.amount && balance == other.balance
    }

    override fun hashCode(): Int {
        return Objects.hash(date, description, amount, balance)
    }

    companion object {
        @JvmStatic
        fun canonicalMoney(value: BigDecimal): BigDecimal {
            requireNotNull(value) { "Money value is required" }
            return value.setScale(2, RoundingMode.HALF_UP)
        }
    }
}