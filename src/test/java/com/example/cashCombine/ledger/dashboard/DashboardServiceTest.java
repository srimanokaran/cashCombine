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
	private Category income;
	private Account everyday;

	@BeforeEach
	void setUp() {
		categoryRepository = new InMemoryCategoryRepository();
		transactionRepository = new InMemoryTransactionRepository();
		accountRepository = new InMemoryAccountRepository();
		uncategorised = categoryRepository.save(Category.uncategorised());
		groceries = categoryRepository.save(Category.create("Groceries"));
		dining = categoryRepository.save(Category.create("Dining"));
		income = categoryRepository.save(Category.create(Category.INCOME_NAME));
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
		save(accountId, "+100.00", uncategorised, "FRIEND PAYBACK", LocalDate.of(2026, 7, 7));
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
		assertThat(dashboard.totalIncome()).isEqualByComparingTo("100.00");
		assertThat(dashboard.incomeCategories()).hasSize(1);
		assertThat(dashboard.incomeCategories().get(0).categoryName()).isEqualTo("Uncategorised");
	}

	@Test
	void incomeAndUncategorisedCreditsCountAsIncomeWhileNamedCreditsOffsetExpenses() {
		AccountId accountId = everyday.id();
		save(accountId, "+3200.00", income, "PAYROLL", LocalDate.of(2026, 7, 10));
		save(accountId, "-80.00", dining, "DINNER WITH FRIENDS", LocalDate.of(2026, 7, 9));
		save(accountId, "+30.00", dining, "FRIEND PAYBACK", LocalDate.of(2026, 7, 8));
		save(accountId, "+50.00", uncategorised, "RANDOM CREDIT", LocalDate.of(2026, 7, 7));
		save(accountId, "+4472.00", fundsBetweenAccounts, "FROM SAVINGS", LocalDate.of(2026, 7, 6));
		save(accountId, "-20.00", groceries, "WOOLWORTHS", LocalDate.of(2026, 7, 5));

		ExpenseDashboard dashboard = dashboardService.expenseBreakdown();

		assertThat(dashboard.totalIncome()).isEqualByComparingTo("3250.00");
		assertThat(dashboard.incomeTransactionCount()).isEqualTo(2);
		assertThat(dashboard.incomeCategories()).hasSize(2);
		assertThat(dashboard.incomeCategories().get(0).categoryName()).isEqualTo("Income");
		assertThat(dashboard.incomeCategories().get(0).amount()).isEqualByComparingTo("3200.00");
		assertThat(dashboard.incomeCategories().get(1).categoryName()).isEqualTo("Uncategorised");
		assertThat(dashboard.incomeCategories().get(1).amount()).isEqualByComparingTo("50.00");

		assertThat(dashboard.totalExpenses()).isEqualByComparingTo("70.00");
		assertThat(dashboard.categories()).hasSize(2);
		assertThat(dashboard.categories().get(0).categoryName()).isEqualTo("Dining");
		assertThat(dashboard.categories().get(0).amount()).isEqualByComparingTo("50.00");
		assertThat(dashboard.categories().get(1).categoryName()).isEqualTo("Groceries");
		assertThat(dashboard.categories().get(1).amount()).isEqualByComparingTo("20.00");
	}

	@Test
	void showsCategoryProfitWhenCreditsExceedSpend() {
		AccountId accountId = everyday.id();
		Category entertainment = categoryRepository.save(Category.create("Entertainment"));
		save(accountId, "-15.26", entertainment, "MOVIE", LocalDate.of(2026, 7, 10));
		save(accountId, "+42.76", entertainment, "FRIEND PAYBACK", LocalDate.of(2026, 7, 11));
		save(accountId, "-50.00", groceries, "WOOLWORTHS", LocalDate.of(2026, 7, 9));

		ExpenseDashboard dashboard = dashboardService.expenseBreakdown();

		assertThat(dashboard.categories()).hasSize(2);
		assertThat(dashboard.categories().get(0).categoryName()).isEqualTo("Groceries");
		assertThat(dashboard.categories().get(0).amount()).isEqualByComparingTo("50.00");
		assertThat(dashboard.categories().get(1).categoryName()).isEqualTo("Entertainment");
		assertThat(dashboard.categories().get(1).amount()).isEqualByComparingTo("-27.50");
		// Entertainment profit offsets total spent: 50 - 27.50 = 22.50
		assertThat(dashboard.totalExpenses()).isEqualByComparingTo("22.50");
	}

	@Test
	void listsExpenseTransactionsIncludingReimbursementsNewestFirst() {
		AccountId accountId = everyday.id();
		save(accountId, "-40.00", groceries, "WOOLWORTHS A", LocalDate.of(2026, 7, 10));
		save(accountId, "-10.00", groceries, "WOOLWORTHS B", LocalDate.of(2026, 7, 11));
		save(accountId, "+5.00", groceries, "REFUND", LocalDate.of(2026, 7, 12));
		save(accountId, "-25.00", dining, "CAFE", LocalDate.of(2026, 7, 8));

		List<ExpenseTransaction> rows = dashboardService.expenseTransactions(groceries.id());

		assertThat(rows).hasSize(3);
		assertThat(rows.get(0).description()).isEqualTo("REFUND");
		assertThat(rows.get(0).amount()).isEqualByComparingTo("5.00");
		assertThat(rows.get(1).description()).isEqualTo("WOOLWORTHS B");
		assertThat(rows.get(1).amount()).isEqualByComparingTo("-10.00");
		assertThat(rows.get(2).description()).isEqualTo("WOOLWORTHS A");
		assertThat(rows.get(2).amount()).isEqualByComparingTo("-40.00");
	}

	@Test
	void listsIncomeTransactionsForUncategorisedCredits() {
		AccountId accountId = everyday.id();
		save(accountId, "+50.00", uncategorised, "FRIEND PAYBACK", LocalDate.of(2026, 7, 12));
		save(accountId, "-20.00", uncategorised, "UNKNOWN SPEND", LocalDate.of(2026, 7, 11));

		List<ExpenseTransaction> incomeRows = dashboardService.incomeTransactions(uncategorised.id());
		List<ExpenseTransaction> expenseRows = dashboardService.expenseTransactions(uncategorised.id());

		assertThat(incomeRows).hasSize(1);
		assertThat(incomeRows.get(0).description()).isEqualTo("FRIEND PAYBACK");
		assertThat(expenseRows).hasSize(1);
		assertThat(expenseRows.get(0).description()).isEqualTo("UNKNOWN SPEND");
	}

	@Test
	void listsIncomeTransactionsForCategoryNewestFirst() {
		AccountId accountId = everyday.id();
		save(accountId, "+3200.00", income, "PAYROLL A", LocalDate.of(2026, 7, 10));
		save(accountId, "+100.00", income, "PAYROLL B", LocalDate.of(2026, 7, 12));
		save(accountId, "-50.00", income, "CORRECTION", LocalDate.of(2026, 7, 11));

		List<ExpenseTransaction> rows = dashboardService.incomeTransactions(income.id());

		assertThat(rows).hasSize(2);
		assertThat(rows.get(0).description()).isEqualTo("PAYROLL B");
		assertThat(rows.get(0).amount()).isEqualByComparingTo("100.00");
		assertThat(rows.get(1).description()).isEqualTo("PAYROLL A");
	}

	@Test
	void emptyLedgerReturnsZero() {
		ExpenseDashboard dashboard = dashboardService.expenseBreakdown();

		assertThat(dashboard.totalExpenses()).isEqualByComparingTo("0.00");
		assertThat(dashboard.expenseTransactionCount()).isZero();
		assertThat(dashboard.categories()).isEmpty();
		assertThat(dashboard.totalIncome()).isEqualByComparingTo("0.00");
		assertThat(dashboard.incomeTransactionCount()).isZero();
		assertThat(dashboard.incomeCategories()).isEmpty();
	}

	@Test
	void cardMerchantsCountTowardExpensesWhileCashCardPaymentsAreExcluded() {
		Category funds = fundsBetweenAccounts;
		Account card = accountRepository.save(Account.create("Qantas Money", AccountType.NAB_CREDIT_CARD));

		// Cash→card payment is a transfer — not spend.
		save(everyday.id(), "-6000.00", funds, "Qantas Credit Cards BPAY", LocalDate.of(2026, 7, 15));
		// Merchants on the card are the real spend.
		save(card.id(), "-45.00", groceries, "WOOLWORTHS 1234", LocalDate.of(2026, 7, 10));
		save(card.id(), "-25.00", dining, "CAFE", LocalDate.of(2026, 7, 9));
		save(everyday.id(), "-20.00", groceries, "COLES", LocalDate.of(2026, 7, 8));

		ExpenseDashboard dashboard = dashboardService.expenseBreakdown();

		assertThat(dashboard.totalExpenses()).isEqualByComparingTo("90.00");
		assertThat(dashboard.expenseTransactionCount()).isEqualTo(3);
		assertThat(dashboard.categories()).extracting(CategorySpend::categoryName)
				.containsExactly("Groceries", "Dining");
		assertThat(dashboard.categories().get(0).amount()).isEqualByComparingTo("65.00");
		assertThat(dashboard.categories().get(1).amount()).isEqualByComparingTo("25.00");

		assertThat(dashboardService.expenseTransactions(groceries.id())).hasSize(2);
		assertThat(dashboardService.expenseTransactions(dining.id())).hasSize(1);
	}

	@Test
	void monthlyCashflowBucketsByMonthAndFillsGaps() {
		AccountId accountId = everyday.id();
		save(accountId, "-40.00", groceries, "WOOLWORTHS MAY", LocalDate.of(2026, 5, 10));
		save(accountId, "+3200.00", income, "PAYROLL MAY", LocalDate.of(2026, 5, 15));
		// June intentionally empty
		save(accountId, "-25.00", dining, "CAFE JUL", LocalDate.of(2026, 7, 8));
		save(accountId, "+50.00", uncategorised, "CREDIT JUL", LocalDate.of(2026, 7, 9));
		save(accountId, "-6000.00", fundsBetweenAccounts, "CARD PAYMENT", LocalDate.of(2026, 7, 10));

		List<MonthlyCashflow> months = dashboardService.monthlyCashflow();

		assertThat(months).hasSize(3);
		assertThat(months.get(0).month()).hasToString("2026-05");
		assertThat(months.get(0).totalExpenses()).isEqualByComparingTo("40.00");
		assertThat(months.get(0).totalIncome()).isEqualByComparingTo("3200.00");
		assertThat(months.get(0).net()).isEqualByComparingTo("3160.00");
		assertThat(months.get(0).expenseTransactionCount()).isEqualTo(1);
		assertThat(months.get(0).incomeTransactionCount()).isEqualTo(1);

		assertThat(months.get(1).month()).hasToString("2026-06");
		assertThat(months.get(1).totalExpenses()).isEqualByComparingTo("0.00");
		assertThat(months.get(1).totalIncome()).isEqualByComparingTo("0.00");
		assertThat(months.get(1).net()).isEqualByComparingTo("0.00");

		assertThat(months.get(2).month()).hasToString("2026-07");
		assertThat(months.get(2).totalExpenses()).isEqualByComparingTo("25.00");
		assertThat(months.get(2).totalIncome()).isEqualByComparingTo("50.00");
		assertThat(months.get(2).net()).isEqualByComparingTo("25.00");
	}

	@Test
	void expenseBreakdownRespectsDateWindow() {
		AccountId accountId = everyday.id();
		save(accountId, "-40.00", groceries, "MAY SPEND", LocalDate.of(2026, 5, 10));
		save(accountId, "-25.00", dining, "JUL SPEND", LocalDate.of(2026, 7, 8));
		save(accountId, "+100.00", income, "JUL PAY", LocalDate.of(2026, 7, 9));

		ExpenseDashboard july = dashboardService.expenseBreakdown(
				new DateWindow(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)));

		assertThat(july.totalExpenses()).isEqualByComparingTo("25.00");
		assertThat(july.categories()).extracting(CategorySpend::categoryName).containsExactly("Dining");
		assertThat(july.totalIncome()).isEqualByComparingTo("100.00");

		List<ExpenseTransaction> julyGroceries = dashboardService.expenseTransactions(
				groceries.id(), new DateWindow(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)));
		List<ExpenseTransaction> julyDining = dashboardService.expenseTransactions(
				dining.id(), new DateWindow(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)));

		assertThat(julyGroceries).isEmpty();
		assertThat(julyDining).hasSize(1);
		assertThat(julyDining.get(0).description()).isEqualTo("JUL SPEND");
	}

	private void save(
			AccountId accountId, String amount, Category category, String description, LocalDate date) {
		transactionRepository.save(Transaction.create(
				accountId,
				new ParsedTransactionRow(date, new BigDecimal(amount), description, new BigDecimal("100.00")),
				category.id()));
	}

}
