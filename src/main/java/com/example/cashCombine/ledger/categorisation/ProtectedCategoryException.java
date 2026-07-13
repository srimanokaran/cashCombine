package com.example.cashCombine.ledger.categorisation;

public class ProtectedCategoryException extends RuntimeException {

	public ProtectedCategoryException(String name) {
		super("Cannot delete protected category: " + name);
	}

}
