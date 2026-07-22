package com.example.cashCombine.ledger.imports;

import java.io.InputStream;
import java.util.List;

public interface TransactionCsvParser {

	List<ParsedTransactionRow> parse(InputStream input);

	ParsedTransactionRow parseLine(String line);

	/** Header or other non-data lines that should be ignored during import. */
	default boolean shouldSkipLine(String line) {
		return false;
	}

}
