package com.example.cashCombine.ledger.categorisation;

import java.util.UUID;

public record CategoryId(UUID value) {

	public CategoryId {
		if (value == null) {
			throw new IllegalArgumentException("Category id is required");
		}
	}

	public static CategoryId generate() {
		return new CategoryId(UUID.randomUUID());
	}

}
