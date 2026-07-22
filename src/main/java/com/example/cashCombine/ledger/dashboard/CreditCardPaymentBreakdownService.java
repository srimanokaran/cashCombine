package com.example.cashCombine.ledger.dashboard;

import com.example.cashCombine.ledger.accounts.Account;
import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.accounts.AccountRepository;
import com.example.cashCombine.ledger.categorisation.Category;
import com.example.cashCombine.ledger.categorisation.CategoryId;
import com.example.cashCombine.ledger.categorisation.CategoryRepository;
import com.example.cashCombine.ledger.transactions.Transaction;
import com.example.cashCombine.ledger.transactions.TransactionId;
import com.example.cashCombine.ledger.transactions.TransactionNotFoundException;
import com.example.cashCombine.ledger.transactions.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class CreditCardPaymentBreakdownService {

	private final TransactionRepository transactionRepository;
	private final CategoryRepository categoryRepository;
	private final AccountRepository accountRepository;

	public CreditCardPaymentBreakdownService(
			TransactionRepository transactionRepository,
			CategoryRepository categoryRepository,
			AccountRepository accountRepository) {
		this.transactionRepository = transactionRepository;
		this.categoryRepository = categoryRepository;
		this.accountRepository = accountRepository;
	}

	/**
	 * Returns merchant detail for a cash-account credit-card payment using an auto date window:
	 * after the previous cash "Credit cards" payment through this payment's date.
	 */
	public CardPaymentBreakdown breakdown(TransactionId paymentId) {
		Transaction payment = transactionRepository
				.findById(paymentId)
				.orElseThrow(() -> new TransactionNotFoundException(paymentId));

		Map<AccountId, Account> accountsById = accountsById();
		Account paymentAccount = accountsById.get(payment.accountId());
		if (paymentAccount == null || paymentAccount.type().isAdvisory()) {
			throw new IllegalArgumentException("Card breakdown is only available for cash-account payments");
		}
		if (payment.amount().signum() >= 0) {
			throw new IllegalArgumentException("Card breakdown is only available for outgoing payments");
		}

		Category paymentCategory = categoryRepository.findById(payment.categoryId()).orElse(null);
		if (paymentCategory == null || !paymentCategory.isCreditCards()) {
			throw new IllegalArgumentException("Card breakdown is only available for Credit cards payments");
		}

		Set<AccountId> advisoryAccountIds = advisoryAccountIds(accountsById);
		if (advisoryAccountIds.isEmpty()) {
			return emptyBreakdown(payment);
		}

		LocalDate windowEnd = payment.date();
		LocalDate windowStartExclusive = previousCashCreditCardPaymentDate(
				payment, paymentCategory.id(), accountsById);

		Map<CategoryId, Category> categoriesById = categoriesById();
		List<CardPaymentMerchant> merchants = new ArrayList<>();
		BigDecimal merchantNet = BigDecimal.ZERO;

		for (Transaction tx : transactionRepository.findAll()) {
			if (!advisoryAccountIds.contains(tx.accountId())) {
				continue;
			}
			if (!inWindow(tx.date(), windowStartExclusive, windowEnd)) {
				continue;
			}
			Category category = categoriesById.get(tx.categoryId());
			if (category != null && category.isExcludedFromExpenses()) {
				// Card-side BPAY / payment thank-you — not merchant spend.
				continue;
			}
			Account account = accountsById.get(tx.accountId());
			BigDecimal amount = tx.amount().setScale(2, RoundingMode.HALF_UP);
			merchants.add(new CardPaymentMerchant(
					tx.id(),
					tx.accountId(),
					account != null ? account.name() : "Unknown",
					tx.date(),
					amount,
					tx.description(),
					tx.categoryId(),
					category != null ? category.name() : "Unknown"));
			merchantNet = merchantNet.add(amount);
		}

		merchants.sort(Comparator.comparing(CardPaymentMerchant::date)
				.reversed()
				.thenComparing(CardPaymentMerchant::description));

		return new CardPaymentBreakdown(
				payment.id(),
				payment.date(),
				payment.amount().setScale(2, RoundingMode.HALF_UP),
				windowStartExclusive,
				windowEnd,
				merchantNet.setScale(2, RoundingMode.HALF_UP),
				List.copyOf(merchants));
	}

	private CardPaymentBreakdown emptyBreakdown(Transaction payment) {
		return new CardPaymentBreakdown(
				payment.id(),
				payment.date(),
				payment.amount().setScale(2, RoundingMode.HALF_UP),
				null,
				payment.date(),
				BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
				List.of());
	}

	private LocalDate previousCashCreditCardPaymentDate(
			Transaction payment, CategoryId creditCardsCategoryId, Map<AccountId, Account> accountsById) {
		Optional<LocalDate> previous = Optional.empty();
		for (Transaction tx : transactionRepository.findAll()) {
			if (tx.id().equals(payment.id())) {
				continue;
			}
			if (!tx.categoryId().equals(creditCardsCategoryId)) {
				continue;
			}
			if (tx.amount().signum() >= 0) {
				continue;
			}
			Account account = accountsById.get(tx.accountId());
			if (account == null || account.type().isAdvisory()) {
				continue;
			}
			if (!tx.date().isBefore(payment.date())) {
				continue;
			}
			previous = previous
					.map(d -> tx.date().isAfter(d) ? tx.date() : d)
					.or(() -> Optional.of(tx.date()));
		}
		return previous.orElse(null);
	}

	private static boolean inWindow(LocalDate date, LocalDate startExclusive, LocalDate endInclusive) {
		if (date.isAfter(endInclusive)) {
			return false;
		}
		return startExclusive == null || date.isAfter(startExclusive);
	}

	private Map<AccountId, Account> accountsById() {
		Map<AccountId, Account> accountsById = new HashMap<>();
		for (Account account : accountRepository.findAll()) {
			accountsById.put(account.id(), account);
		}
		return accountsById;
	}

	private Set<AccountId> advisoryAccountIds(Map<AccountId, Account> accountsById) {
		Set<AccountId> ids = new HashSet<>();
		for (Account account : accountsById.values()) {
			if (account.type().isAdvisory()) {
				ids.add(account.id());
			}
		}
		return ids;
	}

	private Map<CategoryId, Category> categoriesById() {
		Map<CategoryId, Category> categoriesById = new HashMap<>();
		for (Category category : categoryRepository.findAll()) {
			categoriesById.put(category.id(), category);
		}
		return categoriesById;
	}
}
