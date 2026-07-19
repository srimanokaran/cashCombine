package com.example.cashCombine.ledger.categorisation;

public class ClassificationRule {

	private final ClassificationRuleId id;
	private final String pattern;
	private final CategoryId categoryId;

	private ClassificationRule(ClassificationRuleId id, String pattern, CategoryId categoryId) {
		this.id = id;
		this.pattern = pattern;
		this.categoryId = categoryId;
	}

	public static ClassificationRule create(String pattern, CategoryId categoryId) {
		if (pattern == null || pattern.isBlank()) {
			throw new IllegalArgumentException("Classification pattern is required");
		}
		if (categoryId == null) {
			throw new IllegalArgumentException("Category id is required");
		}
		// Preserve leading/trailing spaces — e.g. "BP " must not become "BP" (matches BPAY).
		return new ClassificationRule(ClassificationRuleId.generate(), pattern, categoryId);
	}

	public static ClassificationRule reconstitute(ClassificationRuleId id, String pattern, CategoryId categoryId) {
		return new ClassificationRule(id, pattern, categoryId);
	}

	public ClassificationRuleId id() {
		return id;
	}

	public String pattern() {
		return pattern;
	}

	public CategoryId categoryId() {
		return categoryId;
	}

	public boolean matches(String description) {
		if (description == null) {
			return false;
		}
		return description.toLowerCase().contains(pattern.toLowerCase());
	}

}
