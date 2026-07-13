package com.example.cashCombine.ledger.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.categorisation.Category;
import com.example.cashCombine.ledger.categorisation.InMemoryCategoryRepository;
import com.example.cashCombine.ledger.imports.ParsedTransactionRow;
import com.example.cashCombine.ledger.transactions.InMemoryTransactionRepository;
import com.example.cashCombine.ledger.transactions.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {

	private InMemoryCategoryRepository categoryRepository;
	private InMemoryTransactionRepository transactionRepository;
	private DashboardService dashboardService;
	private Category groceries;
	private Category dining;
	private Category uncategorised;

	@BeforeEach
	void setUp() {
		categoryRepository = new InMemoryCategoryRepository();
		transactionRepository = new InMemoryTransactionRepository();
		uncategorised = categoryRepository.save(Category.uncategorised());
		groceries = categoryRepository.save(Category.create("Groceries"));
		dining = categoryRepository.save(Category.create("Dining"));
		dashboardService = new DashboardService(transactionRepository, categoryRepository);
	}

	@Test
	void aggregatesNegativeAmountsByCategory() {
		AccountId accountId = AccountId.generate();
		save(accountId, "-40.00", groceries);
		save(accountId, "-10.00", groceries);
		save(accountId, "-25.00", dining);
		save(accountId, "+100.00", uncategorised); // income ignored

		ExpenseDashboard dashboard = dashboardService.expenseBreakdown();

		assertThat(dashboard.totalExpenses()).isEqualByComparingTo("75.00");
		assertThat(dashboard.expenseTransactionCount()).isEqualTo(3);
		assertThat(dashboard.categories()).hasSize(2);
		assertThat(dashboard.categories().get(0).categoryName()).isEqualTo("Groceries");
		assertThat(dashboard.categories().get(0).amount()).isEqualByComparingTo("50.00");
		assertThat(dashboard.categories().get(0).percent()).isEqualByComparingTo("66.7");
		assertThat(dashboard.categories().get(1).categoryName()).isEqualTo("Dining");
		assertThat(dashboard.categories().get(1).amount()).isEqualByComparingTo("25.00");
	}

	@Test
	void emptyLedgerReturnsZero() {
		ExpenseDashboard dashboard = dashboardService.expenseBreakdown();

		assertThat(dashboard.totalExpenses()).isEqualByComparingTo("0.00");
		assertThat(dashboard.expenseTransactionCount()).isZero();
		assertThat(dashboard.categories()).isEmpty();
	}

	private void save(AccountId accountId, String amount, Category category) {
		transactionRepository.save(Transaction.create(
				accountId,
				new ParsedTransactionRow(
						LocalDate.of(2026, 7, 10),
						new BigDecimal(amount),
						"TEST",
						new BigDecimal("100.00")),
				category.id()));
	}

}
