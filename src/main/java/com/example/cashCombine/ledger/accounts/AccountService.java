package com.example.cashCombine.ledger.accounts;

public class AccountService {

	private final AccountRepository accountRepository;

	public AccountService(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	public Account createAccount(String name, AccountType type) {
		Account account = Account.create(name, type);
		return accountRepository.save(account);
	}

	public void deleteAccount(AccountId id) {
		if (!accountRepository.existsById(id)) {
			throw new AccountNotFoundException(id);
		}
		accountRepository.deleteById(id);
	}

	public Account getAccount(AccountId id) {
		return accountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
	}

}
