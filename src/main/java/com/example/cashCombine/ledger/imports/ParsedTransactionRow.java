package com.example.cashCombine.ledger.imports;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParsedTransactionRow(
		LocalDate date,
		BigDecimal amount,
		String description,
		BigDecimal balance) {
}
