package com.example.cashCombine.ledger.accounts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryAccountRepository implements AccountRepository {

	private final Map<AccountId, Account> accounts = new HashMap<>();

	@Override
	public Account save(Account account) {
		accounts.put(account.id(), account);
		return account;
	}

	@Override
	public Optional<Account> findById(AccountId id) {
		return Optional.ofNullable(accounts.get(id));
	}

	@Override
	public List<Account> findAll() {
		return new ArrayList<>(accounts.values());
	}

	@Override
	public void deleteById(AccountId id) {
		accounts.remove(id);
	}

	@Override
	public boolean existsById(AccountId id) {
		return accounts.containsKey(id);
	}
}
