package com.example.cashCombine.ledger.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cashCombine.ledger.accounts.Account;
import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.accounts.AccountType;
import com.example.cashCombine.ledger.accounts.InMemoryAccountRepository;
import com.example.cashCombine.ledger.categorisation.Category;
import com.example.cashCombine.ledger.categorisation.InMemoryCategoryRepository;
import com.example.cashCombine.ledger.imports.ParsedTransactionRow;
import com.example.cashCombine.ledger.transactions.InMemoryTransactionRepository;
import com.example.cashCombine.ledger.transactions.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {

	private InMemoryCategoryRepository categoryRepository;
	private InMemoryTransactionRepository transactionRepository;
	private InMemoryAccountRepository accountRepository;
	private DashboardService dashboardService;
	private Category groceries;
	private Category dining;
	private Category fundsBetweenAccounts;
	private Category uncategorised;
	private Account everyday;

	@BeforeEach
	void setUp() {
		categoryRepository = new InMemoryCategoryRepository();
		transactionRepository = new InMemoryTransactionRepository();
		accountRepository = new InMemoryAccountRepository();
		uncategorised = categoryRepository.save(Category.uncategorised());
		groceries = categoryRepository.save(Category.create("Groceries"));
		dining = categoryRepository.save(Category.create("Dining"));
		fundsBetweenAccounts = categoryRepository.save(Category.create(Category.FUNDS_BETWEEN_ACCOUNTS_NAME));
		everyday = accountRepository.save(Account.create("Everyday", AccountType.COMMBANK));
		dashboardService = new DashboardService(transactionRepository, categoryRepository, accountRepository);
	}

	@Test
	void aggregatesNegativeAmountsByCategory() {
		AccountId accountId = everyday.id();
		save(accountId, "-40.00", groceries, "WOOLWORTHS A", LocalDate.of(2026, 7, 10));
		save(accountId, "-10.00", groceries, "WOOLWORTHS B", LocalDate.of(2026, 7, 9));
		save(accountId, "-25.00", dining, "CAFE", LocalDate.of(2026, 7, 8));
		save(accountId, "+100.00", uncategorised, "PAY", LocalDate.of(2026, 7, 7));
		save(accountId, "-4472.00", fundsBetweenAccounts, "SAVINGS", LocalDate.of(2026, 7, 6));

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
	void listsExpenseTransactionsForCategoryNewestFirst() {
		AccountId accountId = everyday.id();
		save(accountId, "-40.00", groceries, "WOOLWORTHS A", LocalDate.of(2026, 7, 10));
		save(accountId, "-10.00", groceries, "WOOLWORTHS B", LocalDate.of(2026, 7, 11));
		save(accountId, "+5.00", groceries, "REFUND", LocalDate.of(2026, 7, 12));
		save(accountId, "-25.00", dining, "CAFE", LocalDate.of(2026, 7, 8));

		List<ExpenseTransaction> rows = dashboardService.expenseTransactions(groceries.id());

		assertThat(rows).hasSize(2);
		assertThat(rows.get(0).description()).isEqualTo("WOOLWORTHS B");
		assertThat(rows.get(0).amount()).isEqualByComparingTo("10.00");
		assertThat(rows.get(0).accountName()).isEqualTo("Everyday");
		assertThat(rows.get(1).description()).isEqualTo("WOOLWORTHS A");
		assertThat(rows.get(1).amount()).isEqualByComparingTo("40.00");
	}

	@Test
	void emptyLedgerReturnsZero() {
		ExpenseDashboard dashboard = dashboardService.expenseBreakdown();

		assertThat(dashboard.totalExpenses()).isEqualByComparingTo("0.00");
		assertThat(dashboard.expenseTransactionCount()).isZero();
		assertThat(dashboard.categories()).isEmpty();
	}

	private void save(
			AccountId accountId, String amount, Category category, String description, LocalDate date) {
		transactionRepository.save(Transaction.create(
				accountId,
				new ParsedTransactionRow(date, new BigDecimal(amount), description, new BigDecimal("100.00")),
				category.id()));
	}

}
