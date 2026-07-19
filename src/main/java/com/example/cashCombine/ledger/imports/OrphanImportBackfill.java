package com.example.cashCombine.ledger.imports;

import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.transactions.Transaction;
import com.example.cashCombine.ledger.transactions.TransactionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Links transactions that pre-date import-batch tracking to a synthetic batch per account
 * so they can be listed and deleted like normal uploads.
 */
public final class OrphanImportBackfill {

	static final String LEGACY_FILENAME = "(imported before tracking)";

	private OrphanImportBackfill() {
	}

	/**
	 * @return number of accounts that received a legacy batch
	 */
	public static int run(TransactionRepository transactionRepository, ImportBatchRepository importBatchRepository) {
		Map<AccountId, List<Transaction>> orphansByAccount = new LinkedHashMap<>();
		for (Transaction transaction : transactionRepository.findAll()) {
			if (transaction.importBatchId() != null) {
				continue;
			}
			orphansByAccount.computeIfAbsent(transaction.accountId(), ignored -> new ArrayList<>()).add(transaction);
		}

		int accountsBackfilled = 0;
		for (Map.Entry<AccountId, List<Transaction>> entry : orphansByAccount.entrySet()) {
			List<Transaction> orphans = entry.getValue();
			ImportBatchId batchId = ImportBatchId.generate();
			ImportBatch batch = ImportBatch.reconstitute(
					batchId,
					entry.getKey(),
					LEGACY_FILENAME,
					Instant.EPOCH,
					orphans.size(),
					0,
					0);
			importBatchRepository.save(batch);

			for (Transaction orphan : orphans) {
				transactionRepository.save(orphan.withImportBatchId(batchId));
			}
			accountsBackfilled++;
		}
		return accountsBackfilled;
	}

}
