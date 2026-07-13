package com.example.cashCombine.ledger.categorisation;

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
		for (ClassificationRule rule : ruleRepository.findAll()) {
			if (rule.matches(description)) {
				return rule.categoryId();
			}
		}
		return uncategorisedId;
	}

}
