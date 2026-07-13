package com.example.cashCombine.ledger.transactions;

import java.util.UUID;

public record TransactionId(UUID value) {

	public TransactionId {
		if (value == null) {
			throw new IllegalArgumentException("Transaction id is required");
		}
	}

	public static TransactionId generate() {
		return new TransactionId(UUID.randomUUID());
	}

}
