package com.example.cashCombine.ledger.categorisation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryClassificationRuleRepository implements ClassificationRuleRepository {

	private final Map<ClassificationRuleId, ClassificationRule> rules = new LinkedHashMap<>();

	@Override
	public ClassificationRule save(ClassificationRule rule) {
		rules.put(rule.id(), rule);
		return rule;
	}

	@Override
	public Optional<ClassificationRule> findById(ClassificationRuleId id) {
		return Optional.ofNullable(rules.get(id));
	}

	@Override
	public List<ClassificationRule> findAll() {
		return new ArrayList<>(rules.values());
	}

	@Override
	public void deleteById(ClassificationRuleId id) {
		rules.remove(id);
	}

}
