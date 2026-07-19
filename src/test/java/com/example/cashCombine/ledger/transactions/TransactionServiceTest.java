package com.example.cashCombine.ledger.transactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.categorisation.Category;
import com.example.cashCombine.ledger.categorisation.CategoryAssignmentSource;
import com.example.cashCombine.ledger.categorisation.CategoryId;
import com.example.cashCombine.ledger.categorisation.CategoryNotFoundException;
import com.example.cashCombine.ledger.categorisation.CategoryRepository;
import com.example.cashCombine.ledger.categorisation.ClassificationRule;
import com.example.cashCombine.ledger.categorisation.ClassificationRuleService;
import com.example.cashCombine.ledger.categorisation.InMemoryCategoryRepository;
import com.example.cashCombine.ledger.categorisation.InMemoryClassificationRuleRepository;
import com.example.cashCombine.ledger.imports.ParsedTransactionRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionServiceTest {

	private static final String CAFE_DESCRIPTION = "CAFE EXAMPLE BLEND FAKETOWN AUS";

	private TransactionRepository transactionRepository;
	private CategoryRepository categoryRepository;
	private InMemoryClassificationRuleRepository ruleRepository;
	private TransactionService transactionService;
	private Category uncategorised;
	private Category groceries;
	private Category dining;

	@BeforeEach
	void setUp() {
		transactionRepository = new InMemoryTransactionRepository();
		categoryRepository = new InMemoryCategoryRepository();
		ruleRepository = new InMemoryClassificationRuleRepository();
		uncategorised = categoryRepository.save(Category.uncategorised());
		groceries = categoryRepository.save(Category.create("Groceries"));
		dining = categoryRepository.save(Category.create("Dining"));
		ClassificationRuleService ruleService =
				new ClassificationRuleService(ruleRepository, categoryRepository);
		transactionService = new TransactionService(transactionRepository, categoryRepository, ruleService);
	}

	@Test
	void changeCategoryOverridesRuleAssignmentAndCreatesRule() {
		Transaction transaction = transactionRepository.save(sampleTransaction(uncategorised.id()));
		assertThat(transaction.categoryAssignmentSource()).isEqualTo(CategoryAssignmentSource.RULE);

		Transaction updated = transactionService.changeCategory(transaction.id(), dining.id());

		assertThat(updated.categoryId()).isEqualTo(dining.id());
		assertThat(updated.isManuallyCategorised()).isTrue();
		assertThat(transactionService.getTransaction(transaction.id()).categoryId()).isEqualTo(dining.id());

		List<ClassificationRule> rules = ruleRepository.findAll();
		assertThat(rules).hasSize(1);
		assertThat(rules.get(0).pattern()).isEqualTo(CAFE_DESCRIPTION);
		assertThat(rules.get(0).categoryId()).isEqualTo(dining.id());
	}

	@Test
	void changeCategoryAppliesRuleToMatchingNonManualTransactions() {
		Transaction primary = transactionRepository.save(sampleTransaction(uncategorised.id()));
		Transaction sibling = transactionRepository.save(sampleTransaction(
				AccountId.generate(), uncategorised.id(), CAFE_DESCRIPTION, LocalDate.of(2026, 7, 11)));
		Transaction otherManual = transactionRepository.save(sampleTransaction(
				AccountId.generate(), uncategorised.id(), CAFE_DESCRIPTION, LocalDate.of(2026, 7, 12)));
		otherManual.changeCategory(groceries.id());
		transactionRepository.save(otherManual);
		Transaction unrelated = transactionRepository.save(sampleTransaction(
				AccountId.generate(), uncategorised.id(), "WOOLWORTHS 1234", LocalDate.of(2026, 7, 13)));

		transactionService.changeCategory(primary.id(), dining.id());

		assertThat(transactionRepository.findById(sibling.id()).orElseThrow().categoryId())
				.isEqualTo(dining.id());
		assertThat(transactionRepository.findById(sibling.id()).orElseThrow().categoryAssignmentSource())
				.isEqualTo(CategoryAssignmentSource.RULE);
		assertThat(transactionRepository.findById(otherManual.id()).orElseThrow().categoryId())
				.isEqualTo(groceries.id());
		assertThat(transactionRepository.findById(unrelated.id()).orElseThrow().categoryId())
				.isEqualTo(uncategorised.id());
	}

	@Test
	void changeCategoryRetargetsExistingSamePatternRule() {
		ruleRepository.save(ClassificationRule.create(CAFE_DESCRIPTION, groceries.id()));
		Transaction transaction = transactionRepository.save(sampleTransaction(uncategorised.id()));

		transactionService.changeCategory(transaction.id(), dining.id());

		List<ClassificationRule> rules = ruleRepository.findAll();
		assertThat(rules).hasSize(1);
		assertThat(rules.get(0).pattern()).isEqualTo(CAFE_DESCRIPTION);
		assertThat(rules.get(0).categoryId()).isEqualTo(dining.id());
	}

	@Test
	void changeCategoryRejectsUnknownTransaction() {
		assertThatThrownBy(() -> transactionService.changeCategory(TransactionId.generate(), groceries.id()))
				.isInstanceOf(TransactionNotFoundException.class);
	}

	@Test
	void changeCategoryRejectsUnknownCategory() {
		Transaction transaction = transactionRepository.save(sampleTransaction(uncategorised.id()));

		assertThatThrownBy(() -> transactionService.changeCategory(transaction.id(), CategoryId.generate()))
				.isInstanceOf(CategoryNotFoundException.class);
	}

	@Test
	void bankFactsRemainUnchangedAfterCategoryOverride() {
		Transaction transaction = transactionRepository.save(sampleTransaction(uncategorised.id()));
		LocalDate originalDate = transaction.date();
		BigDecimal originalAmount = transaction.amount();
		String originalDescription = transaction.description();

		transactionService.changeCategory(transaction.id(), groceries.id());
		Transaction updated = transactionService.getTransaction(transaction.id());

		assertThat(updated.date()).isEqualTo(originalDate);
		assertThat(updated.amount()).isEqualByComparingTo(originalAmount);
		assertThat(updated.description()).isEqualTo(originalDescription);
	}

	@Test
	void listsTransactionsForAccount() {
		AccountId accountId = AccountId.generate();
		Transaction first = transactionRepository.save(sampleTransaction(accountId, uncategorised.id()));
		Transaction second = transactionRepository.save(
				sampleTransaction(accountId, groceries.id(), "WOOLWORTHS 1234", LocalDate.of(2026, 7, 9)));
		transactionRepository.save(sampleTransaction(AccountId.generate(), dining.id()));

		assertThat(transactionService.listByAccount(accountId)).containsExactlyInAnyOrder(first, second);
	}

	private Transaction sampleTransaction(CategoryId categoryId) {
		return sampleTransaction(AccountId.generate(), categoryId);
	}

	private Transaction sampleTransaction(AccountId accountId, CategoryId categoryId) {
		return sampleTransaction(accountId, categoryId, CAFE_DESCRIPTION, LocalDate.of(2026, 7, 10));
	}

	private Transaction sampleTransaction(
			AccountId accountId, CategoryId categoryId, String description, LocalDate date) {
		return Transaction.create(
				accountId,
				new ParsedTransactionRow(date, new BigDecimal("-12.50"), description, new BigDecimal("2467.50")),
				categoryId);
	}

}
