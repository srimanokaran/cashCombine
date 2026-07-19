package com.example.cashCombine.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "import_batches", indexes = @Index(name = "idx_import_batches_account", columnList = "account_id"))
public class ImportBatchJpaEntity {

	@Id
	private UUID id;

	@Column(name = "account_id", nullable = false)
	private UUID accountId;

	@Column(length = 512)
	private String filename;

	@Column(name = "imported_at", nullable = false)
	private Instant importedAt;

	@Column(nullable = false)
	private int accepted;

	@Column(nullable = false)
	private int duplicate;

	@Column(nullable = false)
	private int rejected;

	protected ImportBatchJpaEntity() {
	}

	public ImportBatchJpaEntity(
			UUID id,
			UUID accountId,
			String filename,
			Instant importedAt,
			int accepted,
			int duplicate,
			int rejected) {
		this.id = id;
		this.accountId = accountId;
		this.filename = filename;
		this.importedAt = importedAt;
		this.accepted = accepted;
		this.duplicate = duplicate;
		this.rejected = rejected;
	}

	public UUID getId() {
		return id;
	}

	public UUID getAccountId() {
		return accountId;
	}

	public String getFilename() {
		return filename;
	}

	public Instant getImportedAt() {
		return importedAt;
	}

	public int getAccepted() {
		return accepted;
	}

	public int getDuplicate() {
		return duplicate;
	}

	public int getRejected() {
		return rejected;
	}

}
