package com.example.cashCombine.ledger.transactions;

import com.example.cashCombine.ledger.accounts.AccountId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InMemoryTransactionRepository implements TransactionRepository {

	private record AccountFingerprintKey(AccountId accountId, TransactionFingerprint fingerprint) {
	}

	private final Map<TransactionId, Transaction> transactions = new HashMap<>();
	private final Set<AccountFingerprintKey> fingerprints = new HashSet<>();

	@Override
	public Transaction save(Transaction transaction) {
		transactions.put(transaction.id(), transaction);
		fingerprints.add(new AccountFingerprintKey(transaction.accountId(), transaction.fingerprint()));
		return transaction;
	}

	@Override
	public boolean existsByAccountAndFingerprint(AccountId accountId, TransactionFingerprint fingerprint) {
		return fingerprints.contains(new AccountFingerprintKey(accountId, fingerprint));
	}

	@Override
	public void deleteByAccountId(AccountId accountId) {
		transactions.entrySet().removeIf(entry -> entry.getValue().accountId().equals(accountId));
		fingerprints.removeIf(key -> key.accountId().equals(accountId));
	}
}