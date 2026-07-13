package com.example.cashCombine.api.transactions;

import com.example.cashCombine.ledger.categorisation.CategoryAssignmentSource;
import com.example.cashCombine.ledger.transactions.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
		UUID id,
		UUID accountId,
		LocalDate date,
		BigDecimal amount,
		String description,
		BigDecimal balance,
		UUID categoryId,
		CategoryAssignmentSource categoryAssignmentSource) {

	public static TransactionResponse from(Transaction transaction) {
		return new TransactionResponse(
				transaction.id().value(),
				transaction.accountId().value(),
				transaction.date(),
				transaction.amount(),
				transaction.description(),
				transaction.balance(),
				transaction.categoryId().value(),
				transaction.categoryAssignmentSource());
	}

}
