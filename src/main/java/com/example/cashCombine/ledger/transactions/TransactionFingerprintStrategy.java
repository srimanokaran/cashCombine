package com.example.cashCombine.ledger.transactions;

import com.example.cashCombine.ledger.imports.ParsedTransactionRow;

public interface TransactionFingerprintStrategy {

	TransactionFingerprint fingerprint(ParsedTransactionRow row);

}
