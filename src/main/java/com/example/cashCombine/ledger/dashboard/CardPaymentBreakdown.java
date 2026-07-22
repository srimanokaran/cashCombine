package com.example.cashCombine.ledger.dashboard;

import com.example.cashCombine.ledger.transactions.TransactionId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Merchant-level detail for a cash-account credit-card payment.
 * <p>
 * Auto window: card transactions after the previous cash card payment (exclusive) through this
 * payment's date (inclusive). Card-side payment credits (funds between accounts) are omitted.
 */
public record CardPaymentBreakdown(
		TransactionId paymentId,
		LocalDate paymentDate,
		BigDecimal paymentAmount,
		LocalDate windowStartExclusive,
		LocalDate windowEndInclusive,
		BigDecimal merchantNet,
		List<CardPaymentMerchant> merchants) {
}
