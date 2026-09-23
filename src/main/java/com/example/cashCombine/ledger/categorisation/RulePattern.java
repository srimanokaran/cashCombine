package com.example.cashCombine.ledger.categorisation;

/**
 * Derives a stable contains-match pattern from a bank description.
 * <p>
 * CommBank card lines append {@code Card xxNNNN}, FX amounts, and {@code Value Date},
 * which differ per purchase. Using the full string as a rule only matches that one row.
 */
public final class RulePattern {

	private RulePattern() {
	}

	public static String fromDescription(String description) {
		if (description == null || description.isBlank()) {
			throw new IllegalArgumentException("Classification pattern is required");
		}
		String pattern = description.trim();
		pattern = cutAtIgnoreCase(pattern, " Card xx");
		pattern = cutAtIgnoreCase(pattern, " Value Date:");
		String trimmed = pattern.trim();
		if (trimmed.isEmpty()) {
			return description.trim();
		}
		return trimmed;
	}

	private static String cutAtIgnoreCase(String value, String marker) {
		int index = indexOfIgnoreCase(value, marker);
		if (index < 0) {
			return value;
		}
		return value.substring(0, index);
	}

	private static int indexOfIgnoreCase(String value, String marker) {
		return value.toLowerCase().indexOf(marker.toLowerCase());
	}
}
