package com.example.cashCombine.api.dashboard;

import com.example.cashCombine.ledger.dashboard.CardPaymentBreakdown;
import com.example.cashCombine.ledger.dashboard.CardPaymentMerchant;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CardPaymentBreakdownResponse(
		UUID paymentId,
		LocalDate paymentDate,
		BigDecimal paymentAmount,
		LocalDate windowStartExclusive,
		LocalDate windowEndInclusive,
		BigDecimal merchantNet,
		List<CardPaymentMerchantResponse> merchants) {

	public static CardPaymentBreakdownResponse from(CardPaymentBreakdown breakdown) {
		return new CardPaymentBreakdownResponse(
				breakdown.paymentId().value(),
				breakdown.paymentDate(),
				breakdown.paymentAmount(),
				breakdown.windowStartExclusive(),
				breakdown.windowEndInclusive(),
				breakdown.merchantNet(),
				breakdown.merchants().stream().map(CardPaymentMerchantResponse::from).toList());
	}

	public record CardPaymentMerchantResponse(
			UUID id,
			UUID accountId,
			String accountName,
			LocalDate date,
			BigDecimal amount,
			String description,
			UUID categoryId,
			String categoryName) {

		static CardPaymentMerchantResponse from(CardPaymentMerchant merchant) {
			return new CardPaymentMerchantResponse(
					merchant.id().value(),
					merchant.accountId().value(),
					merchant.accountName(),
					merchant.date(),
					merchant.amount(),
					merchant.description(),
					merchant.categoryId().value(),
					merchant.categoryName());
		}
	}
}
