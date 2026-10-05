package com.example.cashCombine.ledger.dashboard

import java.time.LocalDate

data class DateWindow(val from: LocalDate?, val to: LocalDate?) {

    fun contains(date: LocalDate): Boolean {
        if (from != null && date.isBefore(from)) {
            return false
        }
        if (to != null && date.isAfter(to)) {
            return false
        }
        return true
    }

    companion object {
        @JvmField
        val ALL = DateWindow(null, null)
    }
}