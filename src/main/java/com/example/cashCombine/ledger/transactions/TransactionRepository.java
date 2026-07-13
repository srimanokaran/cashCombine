package com.example.cashCombine.ledger.transactions;

import com.example.cashCombine.ledger.accounts.AccountId;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {

	Transaction save(Transaction transaction);

	Optional<Transaction> findById(TransactionId id);

	List<Transaction> findByAccountId(AccountId accountId);

	boolean existsByAccountAndFingerprint(AccountId accountId, TransactionFingerprint fingerprint);

	void deleteByAccountId(AccountId accountId);

}
