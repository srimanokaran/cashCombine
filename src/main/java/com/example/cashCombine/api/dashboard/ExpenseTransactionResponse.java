package com.example.cashCombine.api.dashboard;

import com.example.cashCombine.ledger.dashboard.ExpenseTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseTransactionResponse(
		UUID id,
		UUID accountId,
		String accountName,
		LocalDate date,
		BigDecimal amount,
		String description) {

	public static ExpenseTransactionResponse from(ExpenseTransaction transaction) {
		return new ExpenseTransactionResponse(
				transaction.id().value(),
				transaction.accountId().value(),
				transaction.accountName(),
				transaction.date(),
				transaction.amount(),
				transaction.description());
	}

}
