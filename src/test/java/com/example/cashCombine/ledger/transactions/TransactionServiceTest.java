package com.example.cashCombine.ledger.transactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.categorisation.Category;
import com.example.cashCombine.ledger.categorisation.CategoryAssignmentSource;
import com.example.cashCombine.ledger.categorisation.CategoryId;
import com.example.cashCombine.ledger.categorisation.CategoryNotFoundException;
import com.example.cashCombine.ledger.categorisation.CategoryRepository;
import com.example.cashCombine.ledger.categorisation.InMemoryCategoryRepository;
import com.example.cashCombine.ledger.imports.ParsedTransactionRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionServiceTest {

	private TransactionRepository transactionRepository;
	private CategoryRepository categoryRepository;
	private TransactionService transactionService;
	private Category uncategorised;
	private Category groceries;
	private Category dining;

	@BeforeEach
	void setUp() {
		transactionRepository = new InMemoryTransactionRepository();
		categoryRepository = new InMemoryCategoryRepository();
		uncategorised = categoryRepository.save(Category.uncategorised());
		groceries = categoryRepository.save(Category.create("Groceries"));
		dining = categoryRepository.save(Category.create("Dining"));
		transactionService = new TransactionService(transactionRepository, categoryRepository);
	}

	@Test
	void changeCategoryOverridesRuleAssignment() {
		Transaction transaction = transactionRepository.save(sampleTransaction(uncategorised.id()));
		assertThat(transaction.categoryAssignmentSource()).isEqualTo(CategoryAssignmentSource.RULE);

		Transaction updated = transactionService.changeCategory(transaction.id(), dining.id());

		assertThat(updated.categoryId()).isEqualTo(dining.id());
		assertThat(updated.isManuallyCategorised()).isTrue();
		assertThat(transactionService.getTransaction(transaction.id()).categoryId()).isEqualTo(dining.id());
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

	private Transaction sampleTransaction(CategoryId categoryId) {
		return Transaction.create(
				AccountId.generate(),
				new ParsedTransactionRow(
						LocalDate.of(2026, 7, 10),
						new BigDecimal("-12.50"),
						"CAFE EXAMPLE BLEND FAKETOWN AUS",
						new BigDecimal("2467.50")),
				categoryId);
	}

}
