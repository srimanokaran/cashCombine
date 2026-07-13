package com.example.cashCombine.infrastructure.persistence;

import com.example.cashCombine.ledger.categorisation.CategoryAssignmentSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
		name = "transactions",
		uniqueConstraints =
				@UniqueConstraint(
						name = "uk_transactions_fingerprint",
						columnNames = {"account_id", "tx_date", "amount", "description", "balance"}),
		indexes = @Index(name = "idx_transactions_account", columnList = "account_id"))
public class TransactionJpaEntity {

	@Id
	private UUID id;

	@Column(name = "account_id", nullable = false)
	private UUID accountId;

	@Column(name = "tx_date", nullable = false)
	private LocalDate date;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal amount;

	@Column(nullable = false, length = 1024)
	private String description;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal balance;

	@Column(name = "category_id", nullable = false)
	private UUID categoryId;

	@Enumerated(EnumType.STRING)
	@Column(name = "category_assignment_source", nullable = false)
	private CategoryAssignmentSource categoryAssignmentSource;

	protected TransactionJpaEntity() {
	}

	public TransactionJpaEntity(
			UUID id,
			UUID accountId,
			LocalDate date,
			BigDecimal amount,
			String description,
			BigDecimal balance,
			UUID categoryId,
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

	public UUID getId() {
		return id;
	}

	public UUID getAccountId() {
		return accountId;
	}

	public LocalDate getDate() {
		return date;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public UUID getCategoryId() {
		return categoryId;
	}

	public CategoryAssignmentSource getCategoryAssignmentSource() {
		return categoryAssignmentSource;
	}

	public void setCategoryId(UUID categoryId) {
		this.categoryId = categoryId;
	}

	public void setCategoryAssignmentSource(CategoryAssignmentSource categoryAssignmentSource) {
		this.categoryAssignmentSource = categoryAssignmentSource;
	}

}
