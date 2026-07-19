package com.example.cashCombine.ledger.imports;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.categorisation.Category;
import com.example.cashCombine.ledger.categorisation.CategoryAssignmentSource;
import com.example.cashCombine.ledger.transactions.InMemoryTransactionRepository;
import com.example.cashCombine.ledger.transactions.Transaction;
import com.example.cashCombine.ledger.transactions.TransactionId;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class OrphanImportBackfillTest {

	@Test
	void groupsOrphansIntoOneLegacyBatchPerAccount() {
		var transactions = new InMemoryTransactionRepository();
		var batches = new InMemoryImportBatchRepository();
		AccountId firstAccount = AccountId.generate();
		AccountId secondAccount = AccountId.generate();
		Category uncategorised = Category.uncategorised();

		transactions.save(orphan(firstAccount, uncategorised, "A", "10.00"));
		transactions.save(orphan(firstAccount, uncategorised, "B", "20.00"));
		transactions.save(orphan(secondAccount, uncategorised, "C", "30.00"));

		int accounts = OrphanImportBackfill.run(transactions, batches);

		assertThat(accounts).isEqualTo(2);
		assertThat(batches.findByAccountId(firstAccount)).hasSize(1);
		assertThat(batches.findByAccountId(secondAccount)).hasSize(1);

		ImportBatch firstBatch = batches.findByAccountId(firstAccount).get(0);
		assertThat(firstBatch.filename()).isEqualTo(OrphanImportBackfill.LEGACY_FILENAME);
		assertThat(firstBatch.accepted()).isEqualTo(2);
		assertThat(transactions.findByAccountId(firstAccount))
				.allMatch(tx -> firstBatch.id().equals(tx.importBatchId()));

		assertThat(OrphanImportBackfill.run(transactions, batches)).isZero();
	}

	private static Transaction orphan(
			AccountId accountId, Category category, String description, String balance) {
		return Transaction.reconstitute(
				TransactionId.generate(),
				accountId,
				null,
				LocalDate.of(2026, 7, 10),
				new BigDecimal("-5.00"),
				description,
				new BigDecimal(balance),
				category.id(),
				CategoryAssignmentSource.RULE);
	}

}
