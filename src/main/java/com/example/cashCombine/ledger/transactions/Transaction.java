package com.example.cashCombine.ledger.transactions;

import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.categorisation.CategoryAssignmentSource;
import com.example.cashCombine.ledger.categorisation.CategoryId;
import com.example.cashCombine.ledger.imports.ImportBatchId;
import com.example.cashCombine.ledger.imports.ParsedTransactionRow;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Transaction {

	private final TransactionId id;
	private final AccountId accountId;
	private final ImportBatchId importBatchId;
	private final LocalDate date;
	private final BigDecimal amount;
	private final String description;
	private final BigDecimal balance;
	private CategoryId categoryId;
	private CategoryAssignmentSource categoryAssignmentSource;

	private Transaction(
			TransactionId id,
			AccountId accountId,
			ImportBatchId importBatchId,
			LocalDate date,
			BigDecimal amount,
			String description,
			BigDecimal balance,
			CategoryId categoryId,
			CategoryAssignmentSource categoryAssignmentSource) {
		this.id = id;
		this.accountId = accountId;
		this.importBatchId = importBatchId;
		this.date = date;
		this.amount = amount;
		this.description = description;
		this.balance = balance;
		this.categoryId = categoryId;
		this.categoryAssignmentSource = categoryAssignmentSource;
	}

	public static Transaction create(AccountId accountId, ParsedTransactionRow row, CategoryId categoryId) {
		return create(accountId, row, categoryId, null);
	}

	public static Transaction create(
			AccountId accountId, ParsedTransactionRow row, CategoryId categoryId, ImportBatchId importBatchId) {
		if (categoryId == null) {
			throw new IllegalArgumentException("Category id is required");
		}
		return new Transaction(
				TransactionId.generate(),
				accountId,
				importBatchId,
				row.date(),
				TransactionFingerprint.canonicalMoney(row.amount()),
				row.description(),
				TransactionFingerprint.canonicalMoney(row.balance()),
				categoryId,
				CategoryAssignmentSource.RULE);
	}

	public static Transaction reconstitute(
			TransactionId id,
			AccountId accountId,
			ImportBatchId importBatchId,
			LocalDate date,
			BigDecimal amount,
			String description,
			BigDecimal balance,
			CategoryId categoryId,
			CategoryAssignmentSource categoryAssignmentSource) {
		return new Transaction(
				id,
				accountId,
				importBatchId,
				date,
				amount,
				description,
				balance,
				categoryId,
				categoryAssignmentSource);
	}

	/** Assigns an import batch when backfilling legacy rows that pre-date batch tracking. */
	public Transaction withImportBatchId(ImportBatchId importBatchId) {
		if (importBatchId == null) {
			throw new IllegalArgumentException("Import batch id is required");
		}
		if (this.importBatchId != null) {
			throw new IllegalStateException("Transaction already belongs to an import batch");
		}
		return new Transaction(
				id,
				accountId,
				importBatchId,
				date,
				amount,
				description,
				balance,
				categoryId,
				categoryAssignmentSource);
	}

	public void changeCategory(CategoryId newCategoryId) {
		if (newCategoryId == null) {
			throw new IllegalArgumentException("Category id is required");
		}
		this.categoryId = newCategoryId;
		this.categoryAssignmentSource = CategoryAssignmentSource.MANUAL;
	}

	/** Moves the transaction to another category without treating it as a manual override. */
	public void reassignCategory(CategoryId newCategoryId) {
		if (newCategoryId == null) {
			throw new IllegalArgumentException("Category id is required");
		}
		this.categoryId = newCategoryId;
	}

	/** Applies a rule-derived category (used when re-analysing imports). */
	public void applyRuleCategory(CategoryId newCategoryId) {
		if (newCategoryId == null) {
			throw new IllegalArgumentException("Category id is required");
		}
		if (isManuallyCategorised()) {
			throw new IllegalStateException("Cannot overwrite a manual category with a rule");
		}
		this.categoryId = newCategoryId;
		this.categoryAssignmentSource = CategoryAssignmentSource.RULE;
	}

	public TransactionId id() {
		return id;
	}

	public AccountId accountId() {
		return accountId;
	}

	public ImportBatchId importBatchId() {
		return importBatchId;
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
