package com.example.cashCombine.ledger.categorisation;

import java.util.List;

public interface ClassificationRuleRepository {

	ClassificationRule save(ClassificationRule rule);

	List<ClassificationRule> findAll();

}
