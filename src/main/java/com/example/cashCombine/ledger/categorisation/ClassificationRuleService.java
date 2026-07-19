package com.example.cashCombine.ledger.categorisation;

import java.util.List;
import java.util.Optional;

public class ClassificationRuleService {

	private final ClassificationRuleRepository ruleRepository;
	private final CategoryRepository categoryRepository;

	public ClassificationRuleService(
			ClassificationRuleRepository ruleRepository, CategoryRepository categoryRepository) {
		this.ruleRepository = ruleRepository;
		this.categoryRepository = categoryRepository;
	}

	public ClassificationRule createRule(String pattern, CategoryId categoryId) {
		if (categoryRepository.findById(categoryId).isEmpty()) {
			throw new CategoryNotFoundException(categoryId);
		}
		ClassificationRule rule = ClassificationRule.create(pattern, categoryId);
		return ruleRepository.save(rule);
	}

	/**
	 * Creates a rule for the pattern, or retargets an existing same-pattern rule (case-insensitive).
	 */
	public ClassificationRule upsertRule(String pattern, CategoryId categoryId) {
		if (categoryRepository.findById(categoryId).isEmpty()) {
			throw new CategoryNotFoundException(categoryId);
		}
		String normalised = pattern == null ? "" : pattern;
		Optional<ClassificationRule> existing = ruleRepository.findAll().stream()
				.filter(rule -> rule.pattern().equalsIgnoreCase(normalised))
				.findFirst();
		if (existing.isPresent()) {
			ClassificationRule current = existing.get();
			if (current.categoryId().equals(categoryId)) {
				return current;
			}
			ruleRepository.deleteById(current.id());
		}
		return ruleRepository.save(ClassificationRule.create(normalised, categoryId));
	}

	public List<ClassificationRule> listRules() {
		return ruleRepository.findAll();
	}

	public void deleteRule(ClassificationRuleId ruleId) {
		if (ruleRepository.findById(ruleId).isEmpty()) {
			throw new RuleNotFoundException(ruleId);
		}
		ruleRepository.deleteById(ruleId);
	}

}
