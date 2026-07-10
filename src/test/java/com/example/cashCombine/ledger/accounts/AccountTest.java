package com.example.cashCombine.ledger.accounts;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AccountTest {

	@Test
	void cannotChangeTypeAfterImportsExist() {
		Account account = Account.create("CommBank Everyday", AccountType.COMMBANK);
		account.markAsImported();

		assertThatThrownBy(() -> account.changeType(AccountType.COMMBANK))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("after imports");
	}

}
