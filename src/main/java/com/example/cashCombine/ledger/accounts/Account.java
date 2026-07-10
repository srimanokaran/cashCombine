package com.example.cashCombine.ledger.accounts;

public class Account {

	private final AccountId id;
	private final String name;
	private AccountType type;
	private boolean hasImports;

	private Account(AccountId id, String name, AccountType type, boolean hasImports) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.hasImports = hasImports;
	}

	public static Account create(String name, AccountType type) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Account name is required");
		}
		if (type == null) {
			throw new IllegalArgumentException("Account type is required");
		}
		return new Account(AccountId.generate(), name.trim(), type, false);
	}

	public AccountId id() {
		return id;
	}

	public String name() {
		return name;
	}

	public AccountType type() {
		return type;
	}

	public boolean hasImports() {
		return hasImports;
	}

	public void markAsImported() {
		this.hasImports = true;
	}

	public void changeType(AccountType newType) {
		if (hasImports) {
			throw new IllegalStateException("Cannot change account type after imports exist");
		}
		if (newType == null) {
			throw new IllegalArgumentException("Account type is required");
		}
		this.type = newType;
	}

}
