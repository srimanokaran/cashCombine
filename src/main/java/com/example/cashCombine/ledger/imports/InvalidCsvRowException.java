package com.example.cashCombine.ledger.imports;

public class InvalidCsvRowException extends RuntimeException {

	public InvalidCsvRowException(String message) {
		super(message);
	}

	public InvalidCsvRowException(String message, Throwable cause) {
		super(message, cause);
	}

}
