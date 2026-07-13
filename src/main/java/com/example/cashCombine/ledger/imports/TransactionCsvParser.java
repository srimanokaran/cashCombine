package com.example.cashCombine.ledger.imports;

import java.io.InputStream;
import java.util.List;

public interface TransactionCsvParser {

	List<ParsedTransactionRow> parse(InputStream input);

	ParsedTransactionRow parseLine(String line);

}
