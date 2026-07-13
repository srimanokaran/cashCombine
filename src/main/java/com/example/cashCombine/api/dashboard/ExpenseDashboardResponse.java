package com.example.cashCombine.api.dashboard;

import com.example.cashCombine.ledger.dashboard.ExpenseDashboard;
import java.math.BigDecimal;
import java.util.List;

public record ExpenseDashboardResponse(
		BigDecimal totalExpenses, int expenseTransactionCount, List<CategorySpendResponse> categories) {

	public static ExpenseDashboardResponse from(ExpenseDashboard dashboard) {
		return new ExpenseDashboardResponse(
				dashboard.totalExpenses(),
				dashboard.expenseTransactionCount(),
				dashboard.categories().stream().map(CategorySpendResponse::from).toList());
	}

}
