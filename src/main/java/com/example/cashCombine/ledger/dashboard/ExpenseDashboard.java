package com.example.cashCombine.ledger.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseDashboard(
		BigDecimal totalExpenses,
		int expenseTransactionCount,
		List<CategorySpend> categories,
		BigDecimal totalIncome,
		int incomeTransactionCount,
		List<CategorySpend> incomeCategories) {
}
