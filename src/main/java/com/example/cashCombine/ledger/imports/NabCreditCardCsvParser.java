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
import java.util.Locale;

/**
 * Parses Qantas Money / NAB credit card CSV exports.
 *
 * Header:
 * {@code Date,Amount,Account Number,,Transaction Type,Transaction Details,Category,Merchant Name,Processed On}
 *
 * Amounts are already signed (spend negative, payment/refund positive).
 * {@code Processed On} is encoded into {@link ParsedTransactionRow#balance()} as epoch-day for fingerprinting
 * when present; otherwise {@code 0}.
 */
public class NabCreditCardCsvParser implements TransactionCsvParser {

	private static final int EXPECTED_COLUMN_COUNT = 9;
	/** Full month names as in older sample exports, e.g. {@code 21 July 26}. */
	private static final DateTimeFormatter FULL_MONTH =
			DateTimeFormatter.ofPattern("d MMMM yy", Locale.ENGLISH);
	/** Abbreviated months as in current Qantas Money exports, e.g. {@code 21 Sep 26}. */
	private static final DateTimeFormatter SHORT_MONTH =
			DateTimeFormatter.ofPattern("d MMM yy", Locale.ENGLISH);

	@Override
	public List<ParsedTransactionRow> parse(InputStream input) {
		try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			return parse(reader);
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to parse CSV", e);
		}
	}

	List<ParsedTransactionRow> parse(BufferedReader bufferedReader) throws IOException {
		List<ParsedTransactionRow> rows = new ArrayList<>();
		boolean firstNonBlank = true;

		String line;
		while ((line = bufferedReader.readLine()) != null) {
			if (line.isBlank()) {
				continue;
			}
			if (firstNonBlank) {
				firstNonBlank = false;
				if (isHeader(line)) {
					continue;
				}
			}
			rows.add(parseLine(line));
		}

		return rows;
	}

	@Override
	public ParsedTransactionRow parseLine(String line) {
		String[] fields = CsvLines.split(line);
		if (fields.length != EXPECTED_COLUMN_COUNT) {
			throw new InvalidCsvRowException(
					"Expected %d columns but found %d".formatted(EXPECTED_COLUMN_COUNT, fields.length));
		}

		try {
			LocalDate date = parseDate(fields[0]);
			BigDecimal amount = new BigDecimal(fields[1].trim());
			String description = fields[5].trim();
			if (description.isEmpty()) {
				throw new InvalidCsvRowException("Transaction details are required: " + line);
			}
			BigDecimal balance = encodeProcessedOn(fields[8]);
			return new ParsedTransactionRow(date, amount, description, balance);
		}
		catch (DateTimeParseException | NumberFormatException | InvalidCsvRowException ex) {
			if (ex instanceof InvalidCsvRowException invalid) {
				throw invalid;
			}
			throw new InvalidCsvRowException("Could not parse row: " + line, ex);
		}
	}

	@Override
	public boolean shouldSkipLine(String line) {
		return isHeader(line);
	}

	private static boolean isHeader(String line) {
		String[] fields = CsvLines.split(line);
		return fields.length > 0 && "Date".equalsIgnoreCase(fields[0].trim());
	}

	private static BigDecimal encodeProcessedOn(String raw) {
		String trimmed = raw == null ? "" : raw.trim();
		if (trimmed.isEmpty()) {
			return BigDecimal.ZERO;
		}
		return BigDecimal.valueOf(parseDate(trimmed).toEpochDay());
	}

	/**
	 * Accepts full month names ({@code July}) and abbreviations ({@code Sep}).
	 * Qantas uses {@code Sept} for September, which Java's {@code MMM} does not recognise.
	 */
	private static LocalDate parseDate(String raw) {
		String trimmed = raw.trim();
		try {
			return LocalDate.parse(trimmed, FULL_MONTH);
		}
		catch (DateTimeParseException ignored) {
			// try abbreviated form
		}
		String normalised = trimmed.replaceAll("(?i)\\bSept\\b", "Sep");
		return LocalDate.parse(normalised, SHORT_MONTH);
	}
}
