package com.example.cashCombine.ledger.dashboard;

import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.categorisation.CategoryId;
import com.example.cashCombine.ledger.transactions.TransactionId;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CardPaymentMerchant(
		TransactionId id,
		AccountId accountId,
		String accountName,
		LocalDate date,
		BigDecimal amount,
		String description,
		CategoryId categoryId,
		String categoryName) {
}
