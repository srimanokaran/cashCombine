package com.example.cashCombine.ledger.transactions;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TransactionFingerprintTest {

	@Test
	void treatsDifferentScalesAsSameFingerprint() {
		LocalDate date = LocalDate.of(2026, 7, 10);
		TransactionFingerprint a = new TransactionFingerprint(
				date, new BigDecimal("-45.0"), "WOOLWORTHS", new BigDecimal("2455.0"));
		TransactionFingerprint b = new TransactionFingerprint(
				date, new BigDecimal("-45.00"), "WOOLWORTHS", new BigDecimal("2455.00"));

		assertThat(a).isEqualTo(b);
		assertThat(a.hashCode()).isEqualTo(b.hashCode());
		assertThat(a.amount()).isEqualByComparingTo("-45.00");
		assertThat(a.balance()).isEqualByComparingTo("2455.00");
	}
}
