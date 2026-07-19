package com.example.cashCombine.ledger.imports;

import com.example.cashCombine.ledger.accounts.AccountId;
import java.util.List;
import java.util.Optional;

public interface ImportBatchRepository {

	ImportBatch save(ImportBatch importBatch);

	Optional<ImportBatch> findById(ImportBatchId id);

	List<ImportBatch> findByAccountId(AccountId accountId);

	void deleteById(ImportBatchId id);

	void deleteByAccountId(AccountId accountId);

}
