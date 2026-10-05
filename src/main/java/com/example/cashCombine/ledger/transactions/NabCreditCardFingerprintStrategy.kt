package com.example.cashCombine.ledger.transactions

import com.example.cashCombine.ledger.imports.ParsedTransactionRow

class NabCreditCardFingerprintStrategy : TransactionFingerprintStrategy {

    override fun fingerprint(row: ParsedTransactionRow): TransactionFingerprint =
        TransactionFingerprint(
            date = row.date,
            description = row.description,
            _amount = row.amount,
            _balance = row.balance)
}