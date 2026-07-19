package com.example.cashCombine.ledger.imports;

import com.example.cashCombine.ledger.accounts.AccountId;
import java.time.Instant;

public class ImportBatch {

	private final ImportBatchId id;
	private final AccountId accountId;
	private final String filename;
	private final Instant importedAt;
	private final int accepted;
	private final int duplicate;
	private final int rejected;

	private ImportBatch(
			ImportBatchId id,
			AccountId accountId,
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

	public static ImportBatch create(
			ImportBatchId id,
			AccountId accountId,
			String filename,
			int accepted,
			int duplicate,
			int rejected) {
		if (id == null) {
			throw new IllegalArgumentException("Import batch id is required");
		}
		if (accountId == null) {
			throw new IllegalArgumentException("Account id is required");
		}
		String normalisedFilename = filename == null || filename.isBlank() ? null : filename.trim();
		return new ImportBatch(id, accountId, normalisedFilename, Instant.now(), accepted, duplicate, rejected);
	}

	public static ImportBatch reconstitute(
			ImportBatchId id,
			AccountId accountId,
			String filename,
			Instant importedAt,
			int accepted,
			int duplicate,
			int rejected) {
		return new ImportBatch(id, accountId, filename, importedAt, accepted, duplicate, rejected);
	}

	public ImportBatchId id() {
		return id;
	}

	public AccountId accountId() {
		return accountId;
	}

	public String filename() {
		return filename;
	}

	public Instant importedAt() {
		return importedAt;
	}

	public int accepted() {
		return accepted;
	}

	public int duplicate() {
		return duplicate;
	}

	public int rejected() {
		return rejected;
	}

}
