package com.example.cashCombine.ledger.imports

import java.math.BigDecimal
import java.time.LocalDate

data class ParsedTransactionRow(
    val date: LocalDate,
    val amount: BigDecimal,
    val description: String,
    val balance: BigDecimal
)