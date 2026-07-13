package com.example.cashCombine.ledger.categorisation;

public class RuleNotFoundException extends RuntimeException {

	public RuleNotFoundException(ClassificationRuleId id) {
		super("Classification rule not found: " + id.value());
	}

}
