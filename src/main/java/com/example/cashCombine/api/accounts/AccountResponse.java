package com.example.cashCombine.api.accounts;

import com.example.cashCombine.ledger.accounts.Account;
import com.example.cashCombine.ledger.accounts.AccountType;
import java.util.UUID;

public record AccountResponse(UUID id, String name, AccountType type, boolean hasImports) {

	public static AccountResponse from(Account account) {
		return new AccountResponse(
				account.id().value(), account.name(), account.type(), account.hasImports());
	}

}
