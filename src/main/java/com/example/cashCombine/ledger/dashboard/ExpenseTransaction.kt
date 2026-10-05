package com.example.cashCombine.ledger.dashboard

import com.example.cashCombine.ledger.accounts.AccountId
import com.example.cashCombine.ledger.transactions.TransactionId
import java.math.BigDecimal
import java.time.LocalDate

data class ExpenseTransaction(
    @get:JvmName("id") val id: TransactionId,
    @get:JvmName("accountId") val accountId: AccountId,
    @get:JvmName("accountName") val accountName: String,
    @get:JvmName("date") val date: LocalDate,
    @get:JvmName("amount") val amount: BigDecimal,
    @get:JvmName("description") val description: String)