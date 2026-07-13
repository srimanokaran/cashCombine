package com.example.cashCombine.ledger.dashboard;

import com.example.cashCombine.ledger.categorisation.Category;
import com.example.cashCombine.ledger.categorisation.CategoryId;
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

	public DashboardService(TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
		this.transactionRepository = transactionRepository;
		this.categoryRepository = categoryRepository;
	}

	/**
	 * Aggregates money-out transactions (negative amounts) across all accounts by category.
	 * Amounts are reported as positive spend totals.
	 */
	public ExpenseDashboard expenseBreakdown() {
		Map<CategoryId, BigDecimal> totals = new HashMap<>();
		Map<CategoryId, Integer> counts = new HashMap<>();
		BigDecimal totalExpenses = BigDecimal.ZERO;
		int expenseCount = 0;

		for (Transaction transaction : transactionRepository.findAll()) {
			if (transaction.amount().signum() >= 0) {
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
		for (Category category : categoryRepository.findAll()) {
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

}
