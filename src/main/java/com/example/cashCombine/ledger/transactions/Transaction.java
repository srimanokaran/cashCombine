package com.example.cashCombine.ledger.transactions;

import com.example.cashCombine.ledger.accounts.AccountId;
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
	private final CategoryId categoryId;

	private Transaction(
			TransactionId id,
			AccountId accountId,
			LocalDate date,
			BigDecimal amount,
			String description,
			BigDecimal balance,
			CategoryId categoryId) {
		this.id = id;
		this.accountId = accountId;
		this.date = date;
		this.amount = amount;
		this.description = description;
		this.balance = balance;
		this.categoryId = categoryId;
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
				categoryId);
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

	public TransactionFingerprint fingerprint() {
		return new TransactionFingerprint(date, amount, description, balance);
	}

}
