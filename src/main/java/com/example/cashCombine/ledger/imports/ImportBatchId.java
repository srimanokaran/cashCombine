package com.example.cashCombine.ledger.imports;

import java.util.UUID;

public record ImportBatchId(UUID value) {

	public ImportBatchId {
		if (value == null) {
			throw new IllegalArgumentException("Import batch id is required");
		}
	}

	public static ImportBatchId generate() {
		return new ImportBatchId(UUID.randomUUID());
	}

}
