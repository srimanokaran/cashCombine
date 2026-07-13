package com.example.cashCombine.ledger.categorisation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InMemoryClassificationRuleRepository implements ClassificationRuleRepository {

	private final Map<ClassificationRuleId, ClassificationRule> rules = new LinkedHashMap<>();

	@Override
	public ClassificationRule save(ClassificationRule rule) {
		rules.put(rule.id(), rule);
		return rule;
	}

	@Override
	public List<ClassificationRule> findAll() {
		return new ArrayList<>(rules.values());
	}

}
