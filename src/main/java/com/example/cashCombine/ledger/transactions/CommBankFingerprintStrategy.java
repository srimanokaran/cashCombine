package com.example.cashCombine.ledger.transactions;

import com.example.cashCombine.ledger.imports.ParsedTransactionRow;

public class CommBankFingerprintStrategy implements TransactionFingerprintStrategy {

	@Override
	public TransactionFingerprint fingerprint(ParsedTransactionRow row) {
		return new TransactionFingerprint(row.date(), row.amount(), row.description(), row.balance());
	}

}
