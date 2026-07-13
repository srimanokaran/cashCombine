package com.example.cashCombine.ledger.categorisation;

public class CategoryNotFoundException extends RuntimeException {

	public CategoryNotFoundException(CategoryId id) {
		super("Category not found: " + id.value());
	}

}
