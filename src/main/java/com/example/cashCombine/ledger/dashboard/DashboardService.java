package com.example.cashCombine.ledger.dashboard;

import com.example.cashCombine.ledger.accounts.Account;
import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.accounts.AccountRepository;
import com.example.cashCombine.ledger.categorisation.Category;
import com.example.cashCombine.ledger.categorisation.CategoryId;
import com.example.cashCombine.ledger.categorisation.CategoryNotFoundException;
import com.example.cashCombine.ledger.categorisation.CategoryRepository;
import com.example.cashCombine.ledger.transactions.Transaction;
import com.example.cashCombine.ledger.transactions.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class DashboardService {

	private final TransactionRepository transactionRepository;
	private final CategoryRepository categoryRepository;
	private final AccountRepository accountRepository;

	public DashboardService(
			TransactionRepository transactionRepository,
			CategoryRepository categoryRepository,
			AccountRepository accountRepository) {
		this.transactionRepository = transactionRepository;
		this.categoryRepository = categoryRepository;
		this.accountRepository = accountRepository;
	}

	/**
	 * Aggregates cashflow across all accounts by category.
	 * <p>
	 * Credits in Income or Uncategorised count as income. Credits filed under a named expense
	 * category (e.g. Entertainment) reduce that category's spend instead. Funds-between-accounts
	 * moves (including cash→card payments) are omitted from both sides. Credit-card merchants
	 * count as normal spend.
	 */
	public ExpenseDashboard expenseBreakdown() {
		Map<CategoryId, Category> categoriesById = categoriesById();
		Map<CategoryId, BigDecimal> expenseNets = new HashMap<>();
		Map<CategoryId, Integer> expenseCounts = new HashMap<>();
		Map<CategoryId, BigDecimal> incomeTotals = new HashMap<>();
		Map<CategoryId, Integer> incomeCounts = new HashMap<>();

		for (Transaction transaction : transactionRepository.findAll()) {
			Category category = categoriesById.get(transaction.categoryId());
			if (category == null || category.isExcludedFromExpenses()) {
				continue;
			}

			if (category.isIncomeCreditCategory() && transaction.amount().signum() > 0) {
				incomeTotals.merge(category.id(), transaction.amount(), BigDecimal::add);
				incomeCounts.merge(category.id(), 1, Integer::sum);
				continue;
			}

			if (category.isIncome()) {
				// Ignore non-credit rows parked on the Income category.
				continue;
			}

			// Expense-side: spend increases the net outflow; reimbursements reduce it.
			expenseNets.merge(category.id(), transaction.amount(), BigDecimal::add);
			expenseCounts.merge(category.id(), 1, Integer::sum);
		}

		Breakdown expenses = toExpenseBreakdown(categoriesById, expenseNets, expenseCounts);
		Breakdown income = toIncomeBreakdown(categoriesById, incomeTotals, incomeCounts);
		return new ExpenseDashboard(
				expenses.total(),
				expenses.count(),
				expenses.categories(),
				income.total(),
				income.count(),
				income.categories());
	}

	/**
	 * Transactions in an expense-side category (spends and reimbursements), newest first.
	 * Amounts keep their sign: negative = spend, positive = credit/reimbursement.
	 * Uncategorised only lists spends — its credits appear under income.
	 */
	public List<ExpenseTransaction> expenseTransactions(CategoryId categoryId) {
		Category category = categoryRepository
				.findById(categoryId)
				.orElseThrow(() -> new CategoryNotFoundException(categoryId));
		if (category.isExcludedFromExpenses() || category.isIncome()) {
			return List.of();
		}
		if (category.isUncategorised()) {
			return listTransactions(categoryId, ListMode.EXPENSE_ONLY);
		}
		return listTransactions(categoryId, ListMode.ALL);
	}

	/**
	 * Credits in Income or Uncategorised, newest first.
	 */
	public List<ExpenseTransaction> incomeTransactions(CategoryId categoryId) {
		Category category = categoryRepository
				.findById(categoryId)
				.orElseThrow(() -> new CategoryNotFoundException(categoryId));
		if (!category.isIncomeCreditCategory()) {
			return List.of();
		}
		return listTransactions(categoryId, ListMode.INCOME_ONLY);
	}

	private enum ListMode {
		ALL,
		INCOME_ONLY,
		EXPENSE_ONLY
	}

	private List<ExpenseTransaction> listTransactions(CategoryId categoryId, ListMode mode) {
		Map<AccountId, String> accountNames = new HashMap<>();
		for (Account account : accountRepository.findAll()) {
			accountNames.put(account.id(), account.name());
		}

		List<ExpenseTransaction> rows = new ArrayList<>();
		for (Transaction transaction : transactionRepository.findAll()) {
			if (!transaction.categoryId().equals(categoryId)) {
				continue;
			}
			int sign = transaction.amount().signum();
			if (mode == ListMode.INCOME_ONLY && sign <= 0) {
				continue;
			}
			if (mode == ListMode.EXPENSE_ONLY && sign >= 0) {
				continue;
			}
			rows.add(new ExpenseTransaction(
					transaction.id(),
					transaction.accountId(),
					accountNames.getOrDefault(transaction.accountId(), "Unknown"),
					transaction.date(),
					transaction.amount().setScale(2, RoundingMode.HALF_UP),
					transaction.description()));
		}

		rows.sort(Comparator.comparing(ExpenseTransaction::date)
				.reversed()
				.thenComparing(ExpenseTransaction::description));
		return List.copyOf(rows);
	}

	private Breakdown toExpenseBreakdown(
			Map<CategoryId, Category> categoriesById,
			Map<CategoryId, BigDecimal> nets,
			Map<CategoryId, Integer> counts) {
		List<CategorySpend> categories = new ArrayList<>();
		BigDecimal signedTotal = BigDecimal.ZERO;
		BigDecimal positiveTotal = BigDecimal.ZERO;
		int count = 0;

		for (Map.Entry<CategoryId, BigDecimal> entry : nets.entrySet()) {
			CategoryId categoryId = entry.getKey();
			int categoryCount = counts.getOrDefault(categoryId, 0);
			if (categoryCount == 0) {
				continue;
			}
			// Ledger sum is negative when money went out. Negating gives net spend;
			// a negative result means the category is in profit (credits > spends).
			BigDecimal netSpend = entry.getValue().negate().setScale(2, RoundingMode.HALF_UP);
			Category category = categoriesById.get(categoryId);
			categories.add(new CategorySpend(
					categoryId,
					category != null ? category.name() : "Unknown",
					netSpend,
					BigDecimal.ZERO,
					categoryCount));
			signedTotal = signedTotal.add(netSpend);
			if (netSpend.signum() > 0) {
				positiveTotal = positiveTotal.add(netSpend);
			}
			count += categoryCount;
		}

		for (int i = 0; i < categories.size(); i++) {
			CategorySpend row = categories.get(i);
			BigDecimal percent = BigDecimal.ZERO;
			if (positiveTotal.signum() > 0 && row.amount().signum() > 0) {
				percent = row.amount()
						.multiply(BigDecimal.valueOf(100))
						.divide(positiveTotal, 1, RoundingMode.HALF_UP);
			}
			categories.set(
					i,
					new CategorySpend(
							row.categoryId(), row.categoryName(), row.amount(), percent, row.transactionCount()));
		}

		categories.sort(Comparator.comparing(CategorySpend::amount)
				.reversed()
				.thenComparing(CategorySpend::categoryName));
		BigDecimal total = signedTotal.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
		return new Breakdown(total, count, List.copyOf(categories));
	}

	private Breakdown toIncomeBreakdown(
			Map<CategoryId, Category> categoriesById,
			Map<CategoryId, BigDecimal> totals,
			Map<CategoryId, Integer> counts) {
		List<CategorySpend> categories = new ArrayList<>();
		BigDecimal total = BigDecimal.ZERO;
		int count = 0;

		for (Map.Entry<CategoryId, BigDecimal> entry : totals.entrySet()) {
			CategoryId categoryId = entry.getKey();
			BigDecimal amount = entry.getValue().setScale(2, RoundingMode.HALF_UP);
			if (amount.signum() <= 0) {
				continue;
			}
			Category category = categoriesById.get(categoryId);
			int categoryCount = counts.getOrDefault(categoryId, 0);
			categories.add(new CategorySpend(
					categoryId,
					category != null ? category.name() : "Unknown",
					amount,
					BigDecimal.ZERO,
					categoryCount));
			total = total.add(amount);
			count += categoryCount;
		}

		for (int i = 0; i < categories.size(); i++) {
			CategorySpend row = categories.get(i);
			BigDecimal percent = total.signum() == 0
					? BigDecimal.ZERO
					: row.amount().multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
			categories.set(
					i,
					new CategorySpend(
							row.categoryId(), row.categoryName(), row.amount(), percent, row.transactionCount()));
		}

		categories.sort(Comparator.comparing(CategorySpend::amount).reversed());
		return new Breakdown(total.setScale(2, RoundingMode.HALF_UP), count, List.copyOf(categories));
	}

	private Map<CategoryId, Category> categoriesById() {
		Map<CategoryId, Category> categoriesById = new HashMap<>();
		for (Category category : categoryRepository.findAll()) {
			categoriesById.put(category.id(), category);
		}
		return categoriesById;
	}

	private record Breakdown(BigDecimal total, int count, List<CategorySpend> categories) {
	}

}
