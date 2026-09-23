package com.example.cashCombine.api.dashboard;

import com.example.cashCombine.ledger.dashboard.MonthlyCashflow;
import java.math.BigDecimal;

public record MonthlyCashflowResponse(
		String month,
		BigDecimal totalExpenses,
		BigDecimal totalIncome,
		BigDecimal net,
		int expenseTransactionCount,
		int incomeTransactionCount) {

	public static MonthlyCashflowResponse from(MonthlyCashflow row) {
		return new MonthlyCashflowResponse(
				row.month().toString(),
				row.totalExpenses(),
				row.totalIncome(),
				row.net(),
				row.expenseTransactionCount(),
				row.incomeTransactionCount());
	}

}
