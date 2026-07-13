package com.example.cashCombine.ledger.categorisation;

import java.util.List;

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
