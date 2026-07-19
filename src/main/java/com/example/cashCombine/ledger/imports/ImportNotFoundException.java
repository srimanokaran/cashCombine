package com.example.cashCombine.ledger.imports;

public class ImportNotFoundException extends RuntimeException {

	public ImportNotFoundException(ImportBatchId id) {
		super("Import not found: " + id.value());
	}

}
