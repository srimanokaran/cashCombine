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
}
