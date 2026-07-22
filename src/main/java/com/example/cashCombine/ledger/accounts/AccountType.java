package com.example.cashCombine.ledger.accounts;

public enum AccountType {
	COMMBANK("CommBank"),
	ING("ING"),
	NAB_CREDIT_CARD("NAB credit card");

	private final String displayName;

	AccountType(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}

	/**
	 * Advisory accounts show individual merchant spend for detail, but their transactions
	 * are omitted from expense/income totals so cash-account card payments are not double-counted.
	 */
	public boolean isAdvisory() {
		return this == NAB_CREDIT_CARD;
	}
}
