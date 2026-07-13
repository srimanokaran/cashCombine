package com.example.cashCombine.ledger.transactions;

import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.categorisation.CategoryId;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {

	Transaction save(Transaction transaction);

	Optional<Transaction> findById(TransactionId id);

	List<Transaction> findByAccountId(AccountId accountId);

	List<Transaction> findByCategoryId(CategoryId categoryId);

	boolean existsByAccountAndFingerprint(AccountId accountId, TransactionFingerprint fingerprint);

	void deleteByAccountId(AccountId accountId);

}
