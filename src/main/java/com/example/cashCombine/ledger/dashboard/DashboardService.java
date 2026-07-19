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
	 * Aggregates money-out transactions (negative amounts) across all accounts by category.
	 * Amounts are reported as positive spend totals. Categories marked excluded from expenses
	 * (e.g. funds moved between own accounts) are omitted.
	 */
	public ExpenseDashboard expenseBreakdown() {
		Map<CategoryId, BigDecimal> totals = new HashMap<>();
		Map<CategoryId, Integer> counts = new HashMap<>();
		BigDecimal totalExpenses = BigDecimal.ZERO;
		int expenseCount = 0;

		Map<CategoryId, Category> categoriesById = new HashMap<>();
		for (Category category : categoryRepository.findAll()) {
			categoriesById.put(category.id(), category);
		}

		for (Transaction transaction : transactionRepository.findAll()) {
			if (transaction.amount().signum() >= 0) {
				continue;
			}
			Category category = categoriesById.get(transaction.categoryId());
			if (category != null && category.isExcludedFromExpenses()) {
				continue;
			}
			BigDecimal spend = transaction.amount().abs();
			CategoryId categoryId = transaction.categoryId();
			totals.merge(categoryId, spend, BigDecimal::add);
			counts.merge(categoryId, 1, Integer::sum);
			totalExpenses = totalExpenses.add(spend);
			expenseCount++;
		}

		Map<CategoryId, String> names = new HashMap<>();
		for (Category category : categoriesById.values()) {
			names.put(category.id(), category.name());
		}

		List<CategorySpend> categories = new ArrayList<>();
		for (Map.Entry<CategoryId, BigDecimal> entry : totals.entrySet()) {
			CategoryId categoryId = entry.getKey();
			BigDecimal amount = entry.getValue().setScale(2, RoundingMode.HALF_UP);
			BigDecimal percent = totalExpenses.signum() == 0
					? BigDecimal.ZERO
					: amount
							.multiply(BigDecimal.valueOf(100))
							.divide(totalExpenses, 1, RoundingMode.HALF_UP);
			categories.add(new CategorySpend(
					categoryId,
					names.getOrDefault(categoryId, "Unknown"),
					amount,
					percent,
					counts.getOrDefault(categoryId, 0)));
		}

		categories.sort(Comparator.comparing(CategorySpend::amount).reversed());

		return new ExpenseDashboard(
				totalExpenses.setScale(2, RoundingMode.HALF_UP), expenseCount, List.copyOf(categories));
	}

	/**
	 * Money-out transactions in a category that contribute to the expenses breakdown.
	 * Amounts are absolute spend totals, newest first.
	 */
	public List<ExpenseTransaction> expenseTransactions(CategoryId categoryId) {
		Category category = categoryRepository
				.findById(categoryId)
				.orElseThrow(() -> new CategoryNotFoundException(categoryId));
		if (category.isExcludedFromExpenses()) {
			return List.of();
		}

		Map<AccountId, String> accountNames = new HashMap<>();
		for (Account account : accountRepository.findAll()) {
			accountNames.put(account.id(), account.name());
		}

		List<ExpenseTransaction> rows = new ArrayList<>();
		for (Transaction transaction : transactionRepository.findAll()) {
			if (!transaction.categoryId().equals(categoryId)) {
				continue;
			}
			if (transaction.amount().signum() >= 0) {
				continue;
			}
			rows.add(new ExpenseTransaction(
					transaction.id(),
					transaction.accountId(),
					accountNames.getOrDefault(transaction.accountId(), "Unknown"),
					transaction.date(),
					transaction.amount().abs().setScale(2, RoundingMode.HALF_UP),
					transaction.description()));
		}

		rows.sort(Comparator.comparing(ExpenseTransaction::date)
				.reversed()
				.thenComparing(ExpenseTransaction::description));
		return List.copyOf(rows);
	}

}
