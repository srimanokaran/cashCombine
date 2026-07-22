package com.example.cashCombine.ledger.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.cashCombine.ledger.accounts.Account;
import com.example.cashCombine.ledger.accounts.AccountType;
import com.example.cashCombine.ledger.accounts.InMemoryAccountRepository;
import com.example.cashCombine.ledger.categorisation.Category;
import com.example.cashCombine.ledger.categorisation.InMemoryCategoryRepository;
import com.example.cashCombine.ledger.imports.ParsedTransactionRow;
import com.example.cashCombine.ledger.transactions.InMemoryTransactionRepository;
import com.example.cashCombine.ledger.transactions.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreditCardPaymentBreakdownServiceTest {

	private InMemoryCategoryRepository categoryRepository;
	private InMemoryTransactionRepository transactionRepository;
	private InMemoryAccountRepository accountRepository;
	private CreditCardPaymentBreakdownService service;
	private Category creditCards;
	private Category groceries;
	private Category dining;
	private Category funds;
	private Account commbank;
	private Account nabCard;

	@BeforeEach
	void setUp() {
		categoryRepository = new InMemoryCategoryRepository();
		transactionRepository = new InMemoryTransactionRepository();
		accountRepository = new InMemoryAccountRepository();
		creditCards = categoryRepository.save(Category.create(Category.CREDIT_CARDS_NAME));
		groceries = categoryRepository.save(Category.create("Groceries"));
		dining = categoryRepository.save(Category.create("Dining"));
		funds = categoryRepository.save(Category.create(Category.FUNDS_BETWEEN_ACCOUNTS_NAME));
		commbank = accountRepository.save(Account.create("Everyday", AccountType.COMMBANK));
		nabCard = accountRepository.save(Account.create("Qantas Money", AccountType.NAB_CREDIT_CARD));
		service = new CreditCardPaymentBreakdownService(
				transactionRepository, categoryRepository, accountRepository);
	}

	@Test
	void linksMerchantsBetweenPreviousPaymentAndThisPayment() {
		Transaction previousPayment = save(
				commbank,
				"-4000.00",
				creditCards,
				"Qantas Credit Cards BPAY June",
				LocalDate.of(2026, 6, 15));
		save(nabCard, "-30.00", groceries, "WOOLWORTHS BEFORE WINDOW", LocalDate.of(2026, 6, 10));
		save(nabCard, "-45.00", groceries, "WOOLWORTHS IN WINDOW", LocalDate.of(2026, 6, 20));
		save(nabCard, "-25.00", dining, "CAFE IN WINDOW", LocalDate.of(2026, 7, 5));
		save(nabCard, "6000.00", funds, "BPAY PAYMENT - THANK YOU", LocalDate.of(2026, 7, 14));
		Transaction payment = save(
				commbank,
				"-6000.00",
				creditCards,
				"Qantas Credit Cards BPAY July",
				LocalDate.of(2026, 7, 15));
		save(nabCard, "-10.00", groceries, "AFTER PAYMENT", LocalDate.of(2026, 7, 16));

		CardPaymentBreakdown breakdown = service.breakdown(payment.id());

		assertThat(breakdown.windowStartExclusive()).isEqualTo(previousPayment.date());
		assertThat(breakdown.windowEndInclusive()).isEqualTo(payment.date());
		assertThat(breakdown.paymentAmount()).isEqualByComparingTo("-6000.00");
		assertThat(breakdown.merchants()).extracting(CardPaymentMerchant::description)
				.containsExactly("CAFE IN WINDOW", "WOOLWORTHS IN WINDOW");
		assertThat(breakdown.merchantNet()).isEqualByComparingTo("-70.00");
	}

	@Test
	void firstPaymentIncludesAllCardMerchantsUpToPaymentDate() {
		save(nabCard, "-45.00", groceries, "WOOLWORTHS", LocalDate.of(2026, 7, 10));
		save(nabCard, "-25.00", dining, "CAFE", LocalDate.of(2026, 7, 9));
		Transaction payment = save(
				commbank,
				"-6000.00",
				creditCards,
				"Qantas Credit Cards BPAY",
				LocalDate.of(2026, 7, 15));

		CardPaymentBreakdown breakdown = service.breakdown(payment.id());

		assertThat(breakdown.windowStartExclusive()).isNull();
		assertThat(breakdown.merchants()).hasSize(2);
		assertThat(breakdown.merchantNet()).isEqualByComparingTo("-70.00");
	}

	@Test
	void rejectsNonCreditCardsCashOutflow() {
		Transaction groceriesSpend = save(
				commbank, "-20.00", groceries, "COLES", LocalDate.of(2026, 7, 8));

		assertThatThrownBy(() -> service.breakdown(groceriesSpend.id()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Credit cards");
	}

	private Transaction save(
			Account account, String amount, Category category, String description, LocalDate date) {
		return transactionRepository.save(Transaction.create(
				account.id(),
				new ParsedTransactionRow(date, new BigDecimal(amount), description, new BigDecimal("100.00")),
				category.id()));
	}
}
