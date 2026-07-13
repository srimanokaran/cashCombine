package com.example.cashCombine.ledger.accounts;

import com.example.cashCombine.ledger.transactions.TransactionRepository;

public class AccountService {

	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;

	public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
	}

	public Account createAccount(String name, AccountType type) {
		Account account = Account.create(name, type);
		return accountRepository.save(account);
	}

	public void deleteAccount(AccountId id) {
		if (!accountRepository.existsById(id)) {
			throw new AccountNotFoundException(id);
		}
		transactionRepository.deleteByAccountId(id);
		accountRepository.deleteById(id);
	}

	public Account getAccount(AccountId id) {
		return accountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
	}

}
