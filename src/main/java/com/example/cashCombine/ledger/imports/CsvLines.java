package com.example.cashCombine.ledger.imports;

import java.util.ArrayList;
import java.util.List;

final class CsvLines {

	private CsvLines() {
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
	static String[] split(String line) {
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
