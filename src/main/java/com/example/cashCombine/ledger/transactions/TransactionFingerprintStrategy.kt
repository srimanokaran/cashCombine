package com.example.cashCombine.ledger.transactions

import com.example.cashCombine.ledger.imports.ParsedTransactionRow

interface TransactionFingerprintStrategy {

    fun fingerprint(row: ParsedTransactionRow): TransactionFingerprint
}