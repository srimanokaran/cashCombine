package com.example.cashCombine.ledger.transactions;

public class TransactionNotFoundException extends RuntimeException {

	public TransactionNotFoundException(TransactionId id) {
		super("Transaction not found: " + id.value());
	}

}
