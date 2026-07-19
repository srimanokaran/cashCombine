package com.example.cashCombine.ledger.categorisation;

public class Category {

	public static final String UNCATEGORISED_NAME = "Uncategorised";

	/** Internal account-to-account moves; excluded from expense totals. */
	public static final String FUNDS_BETWEEN_ACCOUNTS_NAME = "Funds between accounts";

	private final CategoryId id;
	private final String name;

	private Category(CategoryId id, String name) {
		this.id = id;
		this.name = name;
	}

	public static Category create(String name) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Category name is required");
		}
		return new Category(CategoryId.generate(), name.trim());
	}

	public static Category reconstitute(CategoryId id, String name) {
		return new Category(id, name);
	}

	public static Category uncategorised() {
		return create(UNCATEGORISED_NAME);
	}

	public CategoryId id() {
		return id;
	}

	public String name() {
		return name;
	}

	public boolean isUncategorised() {
		return UNCATEGORISED_NAME.equalsIgnoreCase(name);
	}

	public boolean isExcludedFromExpenses() {
		return FUNDS_BETWEEN_ACCOUNTS_NAME.equalsIgnoreCase(name);
	}

}
