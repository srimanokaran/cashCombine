package com.example.cashCombine.ledger.categorisation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionClassifierTest {

	private Category uncategorised;
	private Category groceries;
	private Category streaming;
	private ClassificationRuleRepository ruleRepository;
	private TransactionClassifier classifier;

	@BeforeEach
	void setUp() {
		CategoryRepository categoryRepository = new InMemoryCategoryRepository();
		uncategorised = categoryRepository.save(Category.uncategorised());
		groceries = categoryRepository.save(Category.create("Groceries"));
		streaming = categoryRepository.save(Category.create("Streaming"));

		ruleRepository = new InMemoryClassificationRuleRepository();
		classifier = new TransactionClassifier(ruleRepository, uncategorised.id());
	}

	@Test
	void assignsMatchingRuleCategory() {
		ruleRepository.save(ClassificationRule.create("WOOLWORTHS", groceries.id()));

		assertThat(classifier.classify("WOOLWORTHS 1234 FAKETOWN")).isEqualTo(groceries.id());
	}

	@Test
	void matchingIsCaseInsensitive() {
		ruleRepository.save(ClassificationRule.create("netflix", streaming.id()));

		assertThat(classifier.classify("Netflix.com Melbourne")).isEqualTo(streaming.id());
	}

	@Test
	void fallsBackToUncategorisedWhenNoRuleMatches() {
		ruleRepository.save(ClassificationRule.create("WOOLWORTHS", groceries.id()));

		assertThat(classifier.classify("RANDOM MERCHANT XYZ")).isEqualTo(uncategorised.id());
	}

	@Test
	void usesFirstMatchingRuleInRegistrationOrder() {
		Category transport = Category.create("Transport");
		ruleRepository.save(ClassificationRule.create("UBER", transport.id()));
		ruleRepository.save(ClassificationRule.create("UBER *ONE", streaming.id()));

		assertThat(classifier.classify("UBER *ONE MEMBERSHIP")).isEqualTo(transport.id());
	}

}
