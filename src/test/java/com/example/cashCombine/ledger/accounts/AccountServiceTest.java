package com.example.cashCombine.ledger.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountServiceTest {

	private AccountService accountService;

	@BeforeEach
	void setUp() {
		accountService = new AccountService(new InMemoryAccountRepository());
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
	void rejectsBlankAccountName() {
		assertThatThrownBy(() -> accountService.createAccount("  ", AccountType.COMMBANK))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("name");
	}

}
