package com.example.cashCombine.ledger.categorisation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.imports.ParsedTransactionRow;
import com.example.cashCombine.ledger.transactions.InMemoryTransactionRepository;
import com.example.cashCombine.ledger.transactions.Transaction;
import com.example.cashCombine.ledger.transactions.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoryServiceTest {

	private CategoryRepository categoryRepository;
	private ClassificationRuleRepository ruleRepository;
	private TransactionRepository transactionRepository;
	private CategoryService categoryService;
	private Category uncategorised;

	@BeforeEach
	void setUp() {
		categoryRepository = new InMemoryCategoryRepository();
		ruleRepository = new InMemoryClassificationRuleRepository();
		transactionRepository = new InMemoryTransactionRepository();
		uncategorised = categoryRepository.save(Category.uncategorised());
		categoryService = new CategoryService(categoryRepository, ruleRepository, transactionRepository);
	}

	@Test
	void createsAndListsCategories() {
		Category groceries = categoryService.createCategory("Groceries");
		Category dining = categoryService.createCategory("Dining");

		assertThat(categoryService.listCategories()).containsExactlyInAnyOrder(uncategorised, groceries, dining);
	}

	@Test
	void rejectsDuplicateCategoryNameIgnoringCase() {
		categoryService.createCategory("Groceries");

		assertThatThrownBy(() -> categoryService.createCategory("groceries"))
				.isInstanceOf(DuplicateCategoryNameException.class);
	}

	@Test
	void rejectsBlankCategoryName() {
		assertThatThrownBy(() -> categoryService.createCategory("  "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("name");
	}

	@Test
	void deletesCategoryAndCascadesRulesAndTransactions() {
		Category groceries = categoryService.createCategory("Groceries");
		ruleRepository.save(ClassificationRule.create("WOOLWORTHS", groceries.id()));
		Transaction transaction = transactionRepository.save(Transaction.create(
				AccountId.generate(),
				new ParsedTransactionRow(
						LocalDate.of(2026, 7, 10),
						new BigDecimal("-12.50"),
						"WOOLWORTHS",
						new BigDecimal("100.00")),
				groceries.id()));

		categoryService.deleteCategory(groceries.id());

		assertThat(categoryRepository.findById(groceries.id())).isEmpty();
		assertThat(ruleRepository.findAll()).isEmpty();
		assertThat(transactionRepository.findById(transaction.id())).hasValueSatisfying(updated -> {
			assertThat(updated.categoryId()).isEqualTo(uncategorised.id());
			assertThat(updated.categoryAssignmentSource()).isEqualTo(CategoryAssignmentSource.RULE);
		});
	}

	@Test
	void rejectsDeletingUncategorised() {
		assertThatThrownBy(() -> categoryService.deleteCategory(uncategorised.id()))
				.isInstanceOf(ProtectedCategoryException.class);
	}

	@Test
	void deleteRejectsUnknownCategory() {
		assertThatThrownBy(() -> categoryService.deleteCategory(CategoryId.generate()))
				.isInstanceOf(CategoryNotFoundException.class);
	}

}
