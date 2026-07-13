package com.example.cashCombine.api.dashboard;

import com.example.cashCombine.ledger.dashboard.CategorySpend;
import java.math.BigDecimal;
import java.util.UUID;

public record CategorySpendResponse(
		UUID categoryId, String categoryName, BigDecimal amount, BigDecimal percent, int transactionCount) {

	public static CategorySpendResponse from(CategorySpend spend) {
		return new CategorySpendResponse(
				spend.categoryId().value(),
				spend.categoryName(),
				spend.amount(),
				spend.percent(),
				spend.transactionCount());
	}

}
