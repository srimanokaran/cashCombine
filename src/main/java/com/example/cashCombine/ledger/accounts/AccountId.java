package com.example.cashCombine.ledger.accounts;

import java.util.UUID;

public record AccountId(UUID value) {

	public AccountId {
		if (value == null) {
			throw new IllegalArgumentException("Account id is required");
		}
	}

	public static AccountId generate() {
		return new AccountId(UUID.randomUUID());
	}

}
