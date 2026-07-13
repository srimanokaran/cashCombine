package com.example.cashCombine.api;

import com.example.cashCombine.ledger.accounts.AccountNotFoundException;
import com.example.cashCombine.ledger.categorisation.CategoryNotFoundException;
import com.example.cashCombine.ledger.categorisation.DuplicateCategoryNameException;
import com.example.cashCombine.ledger.imports.InvalidCsvFormatException;
import com.example.cashCombine.ledger.transactions.TransactionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler({
		AccountNotFoundException.class,
		CategoryNotFoundException.class,
		TransactionNotFoundException.class
	})
	public ResponseEntity<ErrorResponse> notFound(RuntimeException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler({InvalidCsvFormatException.class, IllegalArgumentException.class})
	public ResponseEntity<ErrorResponse> badRequest(RuntimeException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler(DuplicateCategoryNameException.class)
	public ResponseEntity<ErrorResponse> conflict(DuplicateCategoryNameException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ErrorResponse> illegalState(IllegalStateException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.orElse("Validation failed");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
	}

}
