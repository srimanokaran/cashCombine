package com.example.cashCombine.ledger.transactions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class TransactionFingerprintTest {

    @Test
    fun `treats different scales as same fingerprint`() {
        val date = LocalDate.of(2026, 7, 10)
        val a = TransactionFingerprint(
            date = date, description = "WOOLWORTHS", _amount = BigDecimal("-45.0"), _balance = BigDecimal("2455.0"))
        val b = TransactionFingerprint(
            date = date, description = "WOOLWORTHS", _amount = BigDecimal("-45.00"), _balance = BigDecimal("2455.00"))

        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
        assertThat(a.amount).isEqualByComparingTo("-45.00")
        assertThat(a.balance).isEqualByComparingTo("2455.00")
    }
}