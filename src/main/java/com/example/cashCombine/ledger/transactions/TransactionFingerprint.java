package com.example.cashCombine.ledger.transactions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Identity for duplicate detection. Amount and balance are normalized to scale 2 so
 * {@code -45.0} and {@code -45.00} fingerprint as the same transaction.
 */
public record TransactionFingerprint(
		LocalDate date,
		BigDecimal amount,
		String description,
		BigDecimal balance) {

	public TransactionFingerprint {
		amount = canonicalMoney(amount);
		balance = canonicalMoney(balance);
	}

	public static BigDecimal canonicalMoney(BigDecimal value) {
		if (value == null) {
			throw new IllegalArgumentException("Money value is required");
		}
		return value.setScale(2, RoundingMode.HALF_UP);
	}
}
