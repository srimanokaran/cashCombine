package com.example.cashCombine.ledger.categorisation;

public class DuplicateCategoryNameException extends RuntimeException {

	public DuplicateCategoryNameException(String name) {
		super("Category already exists: " + name);
	}

}
