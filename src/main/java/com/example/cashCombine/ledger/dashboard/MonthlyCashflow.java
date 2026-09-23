package com.example.cashCombine.ledger.dashboard;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyCashflow(
		YearMonth month,
		BigDecimal totalExpenses,
		BigDecimal totalIncome,
		BigDecimal net,
		int expenseTransactionCount,
		int incomeTransactionCount) {
}
