package com.example.cashCombine.ledger.accounts;

public class AccountNotFoundException extends RuntimeException {

	public AccountNotFoundException(AccountId id) {
		super("Account not found: " + id.value());
	}

}
