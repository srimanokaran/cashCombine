package com.example.cashCombine.ledger.imports;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class CommBankCsvParser {

	private static final int EXPECTED_COLUMN_COUNT = 4;
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	/**
	 * Parses a CommBank CSV export (no header row).
	 *
	 * Input line shape (exactly 4 columns):
	 * {@code 10/07/2026,"-45.00","WOOLWORTHS 1234 FAKETOWN VIC AUS","+2455.00"}
	 *
	 * Columns: date (DD/MM/YYYY), signed amount, description, signed balance.
	 *
	 * Output shape ({@link ParsedTransactionRow}):
	 * {@code date=2026-07-10, amount=-45.00, description="WOOLWORTHS...", balance=2455.00}
	 */
	public List<ParsedTransactionRow> parse(InputStream input) throws IOException {
		try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			return parse(reader);
		}
	}

	/**
	 * Same input/output shape as {@link #parse(InputStream)}.
	 * Does not close {@code bufferedReader} — caller owns the resource.
	 */
	public List<ParsedTransactionRow> parse(BufferedReader bufferedReader) throws IOException {
		List<ParsedTransactionRow> rows = new ArrayList<>();

		String line;
		while ((line = bufferedReader.readLine()) != null) {
			if (line.isBlank()) {
				continue;
			}
			rows.add(parseLine(line));
		}

		return rows;
	}

	public ParsedTransactionRow parseLine(String line) {
		String[] fields = splitCsvLine(line);
		if (fields.length != EXPECTED_COLUMN_COUNT) {
			throw new InvalidCsvRowException(
					"Expected %d columns but found %d".formatted(EXPECTED_COLUMN_COUNT, fields.length));
		}

		try {
			LocalDate date = LocalDate.parse(fields[0].trim(), DATE_FORMAT);
			BigDecimal amount = parseSignedAmount(fields[1]);
			String description = fields[2].trim();
			BigDecimal balance = parseSignedAmount(fields[3]);
			return new ParsedTransactionRow(date, amount, description, balance);
		}
		catch (DateTimeParseException | NumberFormatException ex) {
			throw new InvalidCsvRowException("Could not parse row: " + line, ex);
		}
	}

	private static BigDecimal parseSignedAmount(String raw) {
		String normalized = raw.trim().replace("\"", "");
		return new BigDecimal(normalized);
	}

	/**
	 * Splits one CSV line into fields, respecting quoted commas.
	 *
	 * Input:
	 * {@code 10/07/2026,"-45.00","WOOLWORTHS 1234, FAKETOWN","+2455.00"}
	 *
	 * Output (quotes removed, commas inside quotes kept):
	 * {@code ["10/07/2026", "-45.00", "WOOLWORTHS 1234, FAKETOWN", "+2455.00"]}
	 */
	static String[] splitCsvLine(String line) {
		List<String> fields = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;

		for (int i = 0; i < line.length(); i++) {
			char character = line.charAt(i);
			if (character == '"') {
				inQuotes = !inQuotes;
				continue;
			}
			if (character == ',' && !inQuotes) {
				fields.add(current.toString());
				current = new StringBuilder();
				continue;
			}
			current.append(character);
		}

		fields.add(current.toString());
		return fields.toArray(String[]::new);
	}

}
