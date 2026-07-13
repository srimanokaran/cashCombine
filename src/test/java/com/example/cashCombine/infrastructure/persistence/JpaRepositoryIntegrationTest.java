package com.example.cashCombine.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cashCombine.ledger.accounts.Account;
import com.example.cashCombine.ledger.accounts.AccountRepository;
import com.example.cashCombine.ledger.accounts.AccountType;
import com.example.cashCombine.ledger.categorisation.Category;
import com.example.cashCombine.ledger.categorisation.CategoryRepository;
import com.example.cashCombine.ledger.imports.ParsedTransactionRow;
import com.example.cashCombine.ledger.transactions.Transaction;
import com.example.cashCombine.ledger.transactions.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class JpaRepositoryIntegrationTest {

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Test
	void savesFindsAndDeletesAccountWithTransactions() {
		Category uncategorised = categoryRepository
				.findByName(Category.UNCATEGORISED_NAME)
				.orElseGet(() -> categoryRepository.save(Category.uncategorised()));

		Account account = accountRepository.save(Account.create("Everyday", AccountType.COMMBANK));
		Transaction transaction = transactionRepository.save(Transaction.create(
				account.id(),
				new ParsedTransactionRow(
						LocalDate.of(2026, 7, 10),
						new BigDecimal("-15.26"),
						"TEST MERCHANT",
						new BigDecimal("1000.00")),
				uncategorised.id()));

		assertThat(accountRepository.findById(account.id())).isPresent();
		assertThat(transactionRepository.findByAccountId(account.id())).hasSize(1);
		assertThat(transactionRepository.existsByAccountAndFingerprint(account.id(), transaction.fingerprint()))
				.isTrue();

		transactionRepository.deleteByAccountId(account.id());
		accountRepository.deleteById(account.id());

		assertThat(accountRepository.existsById(account.id())).isFalse();
		assertThat(transactionRepository.findByAccountId(account.id())).isEmpty();
		assertThat(transactionRepository.existsByAccountAndFingerprint(account.id(), transaction.fingerprint()))
				.isFalse();
	}

	@Test
	void seedsUncategorisedCategory() {
		assertThat(categoryRepository.findByName(Category.UNCATEGORISED_NAME)).isPresent();
	}

}
