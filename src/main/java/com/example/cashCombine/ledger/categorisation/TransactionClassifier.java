package com.example.cashCombine.ledger.categorisation;

import java.util.Comparator;

public class TransactionClassifier {

	private final ClassificationRuleRepository ruleRepository;
	private final CategoryId uncategorisedId;

	public TransactionClassifier(ClassificationRuleRepository ruleRepository, CategoryId uncategorisedId) {
		if (uncategorisedId == null) {
			throw new IllegalArgumentException("Uncategorised category id is required");
		}
		this.ruleRepository = ruleRepository;
		this.uncategorisedId = uncategorisedId;
	}

	public CategoryId classify(String description) {
		// Longer patterns first so specific/manual rules beat broader seed rules.
		// Stable sort preserves createdOrder for equal-length patterns.
		return ruleRepository.findAll().stream()
				.sorted(Comparator.comparingInt((ClassificationRule r) -> r.pattern().length()).reversed())
				.filter(rule -> rule.matches(description))
				.map(ClassificationRule::categoryId)
				.findFirst()
				.orElse(uncategorisedId);
	}

}
