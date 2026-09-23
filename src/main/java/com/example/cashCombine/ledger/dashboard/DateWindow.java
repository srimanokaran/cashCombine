package com.example.cashCombine.ledger.dashboard;

import java.time.LocalDate;

/**
 * Inclusive date filter. Null bounds mean unbounded on that side.
 */
public record DateWindow(LocalDate from, LocalDate to) {

	public static final DateWindow ALL = new DateWindow(null, null);

	public boolean contains(LocalDate date) {
		if (from != null && date.isBefore(from)) {
			return false;
		}
		if (to != null && date.isAfter(to)) {
			return false;
		}
		return true;
	}

}
