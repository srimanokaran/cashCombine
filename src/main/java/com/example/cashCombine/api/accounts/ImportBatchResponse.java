package com.example.cashCombine.api.accounts;

import com.example.cashCombine.ledger.imports.ImportBatch;
import java.time.Instant;
import java.util.UUID;

public record ImportBatchResponse(
		UUID id,
		UUID accountId,
		String filename,
		Instant importedAt,
		int accepted,
		int duplicate,
		int rejected) {

	public static ImportBatchResponse from(ImportBatch batch) {
		return new ImportBatchResponse(
				batch.id().value(),
				batch.accountId().value(),
				batch.filename(),
				batch.importedAt(),
				batch.accepted(),
				batch.duplicate(),
				batch.rejected());
	}

}
