package com.example.cashCombine.config;

import com.example.cashCombine.ledger.imports.ImportBatchRepository;
import com.example.cashCombine.ledger.imports.OrphanImportBackfill;
import com.example.cashCombine.ledger.transactions.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-shot backfill: attach pre-tracking transactions to a deletable legacy import batch.
 */
@Component
@Order(2)
public class ImportBatchBackfillRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger("api");

	private final TransactionRepository transactionRepository;
	private final ImportBatchRepository importBatchRepository;

	public ImportBatchBackfillRunner(
			TransactionRepository transactionRepository, ImportBatchRepository importBatchRepository) {
		this.transactionRepository = transactionRepository;
		this.importBatchRepository = importBatchRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		int accounts = OrphanImportBackfill.run(transactionRepository, importBatchRepository);
		if (accounts > 0) {
			log.info("Backfilled legacy import batches for {} account(s)", accounts);
		}
	}

}
