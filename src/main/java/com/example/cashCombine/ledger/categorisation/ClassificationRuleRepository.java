package com.example.cashCombine.ledger.categorisation;

import java.util.List;
import java.util.Optional;

public interface ClassificationRuleRepository {

	ClassificationRule save(ClassificationRule rule);

	Optional<ClassificationRule> findById(ClassificationRuleId id);

	List<ClassificationRule> findAll();

	void deleteById(ClassificationRuleId id);

}
