package com.example.cashCombine.ledger.categorisation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClassificationRuleServiceTest {

	private CategoryRepository categoryRepository;
	private ClassificationRuleService ruleService;
	private Category groceries;

	@BeforeEach
	void setUp() {
		categoryRepository = new InMemoryCategoryRepository();
		groceries = categoryRepository.save(Category.create("Groceries"));
		ruleService = new ClassificationRuleService(new InMemoryClassificationRuleRepository(), categoryRepository);
	}

	@Test
	void createsAndListsRules() {
		ClassificationRule rule = ruleService.createRule("WOOLWORTHS", groceries.id());

		assertThat(rule.pattern()).isEqualTo("WOOLWORTHS");
		assertThat(rule.categoryId()).isEqualTo(groceries.id());
		assertThat(ruleService.listRules()).containsExactly(rule);
	}

	@Test
	void rejectsUnknownCategory() {
		assertThatThrownBy(() -> ruleService.createRule("WOOLWORTHS", CategoryId.generate()))
				.isInstanceOf(CategoryNotFoundException.class);
	}

	@Test
	void rejectsBlankPattern() {
		assertThatThrownBy(() -> ruleService.createRule("  ", groceries.id()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("pattern");
	}

	@Test
	void deletesExistingRule() {
		ClassificationRule rule = ruleService.createRule("WOOLWORTHS", groceries.id());

		ruleService.deleteRule(rule.id());

		assertThat(ruleService.listRules()).isEmpty();
	}

	@Test
	void deleteRejectsUnknownRule() {
		assertThatThrownBy(() -> ruleService.deleteRule(ClassificationRuleId.generate()))
				.isInstanceOf(RuleNotFoundException.class);
	}

}
