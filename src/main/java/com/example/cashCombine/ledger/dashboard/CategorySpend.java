package com.example.cashCombine.ledger.dashboard;

import com.example.cashCombine.ledger.categorisation.CategoryId;
import java.math.BigDecimal;

public record CategorySpend(
		CategoryId categoryId,
		String categoryName,
		BigDecimal amount,
		BigDecimal percent,
		int transactionCount) {
}
