package com.example.cashCombine.ledger.transactions;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionFingerprint(
		LocalDate date,
		BigDecimal amount,
		String description,
		BigDecimal balance) {
}
