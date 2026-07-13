package com.example.cashCombine.ledger.categorisation;

import java.util.UUID;

public record ClassificationRuleId(UUID value) {

	public ClassificationRuleId {
		if (value == null) {
			throw new IllegalArgumentException("Classification rule id is required");
		}
	}

	public static ClassificationRuleId generate() {
		return new ClassificationRuleId(UUID.randomUUID());
	}

}
