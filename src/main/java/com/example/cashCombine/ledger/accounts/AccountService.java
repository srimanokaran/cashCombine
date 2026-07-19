package com.example.cashCombine.ledger.accounts;

import com.example.cashCombine.ledger.imports.ImportBatchRepository;
import com.example.cashCombine.ledger.transactions.TransactionRepository;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AccountService {

	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;
	private final ImportBatchRepository importBatchRepository;

	public AccountService(
			AccountRepository accountRepository,
			TransactionRepository transactionRepository,
			ImportBatchRepository importBatchRepository) {
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
		this.importBatchRepository = importBatchRepository;
	}

	public Account createAccount(String name, AccountType type) {
		Account account = Account.create(name, type);
		return accountRepository.save(account);
	}

	/**
	 * Ensures the fixed bank accounts exist (one per {@link AccountType}) and returns them
	 * in display order.
	 */
	public synchronized List<Account> ensureFixedAccounts() {
		for (AccountType type : AccountType.values()) {
			if (accountRepository.findByType(type).isEmpty()) {
				accountRepository.save(Account.create(type.displayName(), type));
			}
		}
		return listFixedAccounts();
	}

	public List<Account> listFixedAccounts() {
		return Arrays.stream(AccountType.values())
				.map(accountRepository::findByType)
				.flatMap(java.util.Optional::stream)
				.sorted(Comparator.comparingInt(account -> account.type().ordinal()))
				.toList();
	}

	public List<Account> listAccounts() {
		return accountRepository.findAll();
	}

	public void deleteAccount(AccountId id) {
		if (!accountRepository.existsById(id)) {
			throw new AccountNotFoundException(id);
		}
		transactionRepository.deleteByAccountId(id);
		importBatchRepository.deleteByAccountId(id);
		accountRepository.deleteById(id);
	}

	public Account getAccount(AccountId id) {
		return accountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
	}

}
