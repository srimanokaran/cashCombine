package com.example.cashCombine.api.rules;

import com.example.cashCombine.ledger.categorisation.ClassificationRule;
import java.util.UUID;

public record RuleResponse(UUID id, String pattern, UUID categoryId) {

	public static RuleResponse from(ClassificationRule rule) {
		return new RuleResponse(rule.id().value(), rule.pattern(), rule.categoryId().value());
	}

}
