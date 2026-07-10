package com.example.cashCombine.ledger.imports;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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

	public List<ParsedTransactionRow> parse(InputStream input) throws IOException {
		try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			return parse(reader);
		}
	}

	public List<ParsedTransactionRow> parse(Reader reader) throws IOException {
		List<ParsedTransactionRow> rows = new ArrayList<>();
		try (var bufferedReader = reader instanceof BufferedReader br ? br : new BufferedReader(reader)) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}
				rows.add(parseLine(line));
			}
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
