package com.example.cashCombine.ledger.imports;

import com.example.cashCombine.ledger.accounts.AccountId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryImportBatchRepository implements ImportBatchRepository {

	private final Map<ImportBatchId, ImportBatch> batches = new HashMap<>();

	@Override
	public ImportBatch save(ImportBatch importBatch) {
		batches.put(importBatch.id(), importBatch);
		return importBatch;
	}

	@Override
	public Optional<ImportBatch> findById(ImportBatchId id) {
		return Optional.ofNullable(batches.get(id));
	}

	@Override
	public List<ImportBatch> findByAccountId(AccountId accountId) {
		return batches.values().stream()
				.filter(batch -> batch.accountId().equals(accountId))
				.sorted(Comparator.comparing(ImportBatch::importedAt).reversed())
				.toList();
	}

	@Override
	public void deleteById(ImportBatchId id) {
		batches.remove(id);
	}

	@Override
	public void deleteByAccountId(AccountId accountId) {
		batches.entrySet().removeIf(entry -> entry.getValue().accountId().equals(accountId));
	}

}
