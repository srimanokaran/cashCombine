package com.example.cashCombine.ledger.transactions;

import com.example.cashCombine.ledger.accounts.AccountId;

public interface TransactionRepository {

	Transaction save(Transaction transaction);

	boolean existsByAccountAndFingerprint(AccountId accountId, TransactionFingerprint fingerprint);

	void deleteByAccountId(AccountId accountId);

}
