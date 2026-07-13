package com.example.cashCombine.ledger.transactions;

import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.categorisation.CategoryAssignmentSource;
import com.example.cashCombine.ledger.categorisation.CategoryId;
import com.example.cashCombine.ledger.imports.ParsedTransactionRow;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Transaction {

	private final TransactionId id;
	private final AccountId accountId;
	private final LocalDate date;
	private final BigDecimal amount;
	private final String description;
	private final BigDecimal balance;
	private CategoryId categoryId;
	private CategoryAssignmentSource categoryAssignmentSource;

	private Transaction(
			TransactionId id,
			AccountId accountId,
			LocalDate date,
			BigDecimal amount,
			String description,
			BigDecimal balance,
			CategoryId categoryId,
			CategoryAssignmentSource categoryAssignmentSource) {
		this.id = id;
		this.accountId = accountId;
		this.date = date;
		this.amount = amount;
		this.description = description;
		this.balance = balance;
		this.categoryId = categoryId;
		this.categoryAssignmentSource = categoryAssignmentSource;
	}

	public static Transaction create(AccountId accountId, ParsedTransactionRow row, CategoryId categoryId) {
		if (categoryId == null) {
			throw new IllegalArgumentException("Category id is required");
		}
		return new Transaction(
				TransactionId.generate(),
				accountId,
				row.date(),
				row.amount(),
				row.description(),
				row.balance(),
				categoryId,
				CategoryAssignmentSource.RULE);
	}

	public static Transaction reconstitute(
			TransactionId id,
			AccountId accountId,
			LocalDate date,
			BigDecimal amount,
			String description,
			BigDecimal balance,
			CategoryId categoryId,
			CategoryAssignmentSource categoryAssignmentSource) {
		return new Transaction(
				id, accountId, date, amount, description, balance, categoryId, categoryAssignmentSource);
	}

	public void changeCategory(CategoryId newCategoryId) {
		if (newCategoryId == null) {
			throw new IllegalArgumentException("Category id is required");
		}
		this.categoryId = newCategoryId;
		this.categoryAssignmentSource = CategoryAssignmentSource.MANUAL;
	}

	public TransactionId id() {
		return id;
	}

	public AccountId accountId() {
		return accountId;
	}

	public LocalDate date() {
		return date;
	}

	public BigDecimal amount() {
		return amount;
	}

	public String description() {
		return description;
	}

	public BigDecimal balance() {
		return balance;
	}

	public CategoryId categoryId() {
		return categoryId;
	}

	public CategoryAssignmentSource categoryAssignmentSource() {
		return categoryAssignmentSource;
	}

	public boolean isManuallyCategorised() {
		return categoryAssignmentSource == CategoryAssignmentSource.MANUAL;
	}

	public TransactionFingerprint fingerprint() {
		return new TransactionFingerprint(date, amount, description, balance);
	}

}
