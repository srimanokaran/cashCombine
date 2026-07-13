package com.example.cashCombine.ledger.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.cashCombine.ledger.categorisation.Category;
import com.example.cashCombine.ledger.transactions.InMemoryTransactionRepository;
import com.example.cashCombine.ledger.transactions.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountServiceTest {

	private AccountService accountService;
	private InMemoryTransactionRepository transactionRepository;

	@BeforeEach
	void setUp() {
		var accountRepository = new InMemoryAccountRepository();
		transactionRepository = new InMemoryTransactionRepository();
		accountService = new AccountService(accountRepository, transactionRepository);
	}

	@Test
	void createsAccountWithNameAndType() {
		Account account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK);

		assertThat(account.name()).isEqualTo("CommBank Everyday");
		assertThat(account.type()).isEqualTo(AccountType.COMMBANK);
		assertThat(account.hasImports()).isFalse();
		assertThat(account.id()).isNotNull();
	}

	@Test
	void deletesExistingAccount() {
		Account account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK);

		accountService.deleteAccount(account.id());

		assertThatThrownBy(() -> accountService.getAccount(account.id()))
				.isInstanceOf(AccountNotFoundException.class);
	}

	@Test
	void deletesTransactionsWhenAccountIsDeleted() {
		Account account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK);
		Category uncategorised = Category.uncategorised();
		Transaction transaction = Transaction.create(
				account.id(),
				new com.example.cashCombine.ledger.imports.ParsedTransactionRow(
						java.time.LocalDate.of(2026, 7, 10),
						new java.math.BigDecimal("-45.00"),
						"WOOLWORTHS",
						new java.math.BigDecimal("2455.00")),
				uncategorised.id());
		transactionRepository.save(transaction);

		accountService.deleteAccount(account.id());

		assertThat(transactionRepository.existsByAccountAndFingerprint(account.id(), transaction.fingerprint()))
				.isFalse();
	}

	@Test
	void rejectsBlankAccountName() {
		assertThatThrownBy(() -> accountService.createAccount("  ", AccountType.COMMBANK))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("name");
	}

	@Test
	void listsCreatedAccounts() {
		Account first = accountService.createAccount("Everyday", AccountType.COMMBANK);
		Account second = accountService.createAccount("Savings", AccountType.COMMBANK);

		assertThat(accountService.listAccounts()).containsExactlyInAnyOrder(first, second);
	}

}
