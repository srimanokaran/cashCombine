package com.example.cashCombine.infrastructure.persistence;

import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.imports.ImportBatch;
import com.example.cashCombine.ledger.imports.ImportBatchId;
import com.example.cashCombine.ledger.imports.ImportBatchRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JpaImportBatchRepository implements ImportBatchRepository {

	private final ImportBatchJpaRepository jpaRepository;

	public JpaImportBatchRepository(ImportBatchJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public ImportBatch save(ImportBatch importBatch) {
		ImportBatchJpaEntity entity = new ImportBatchJpaEntity(
				importBatch.id().value(),
				importBatch.accountId().value(),
				importBatch.filename(),
				importBatch.importedAt(),
				importBatch.accepted(),
				importBatch.duplicate(),
				importBatch.rejected());
		jpaRepository.save(entity);
		return importBatch;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ImportBatch> findById(ImportBatchId id) {
		return jpaRepository.findById(id.value()).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ImportBatch> findByAccountId(AccountId accountId) {
		return jpaRepository.findByAccountIdOrderByImportedAtDesc(accountId.value()).stream()
				.map(this::toDomain)
				.toList();
	}

	@Override
	public void deleteById(ImportBatchId id) {
		jpaRepository.deleteById(id.value());
	}

	@Override
	public void deleteByAccountId(AccountId accountId) {
		jpaRepository.deleteByAccountId(accountId.value());
	}

	private ImportBatch toDomain(ImportBatchJpaEntity entity) {
		return ImportBatch.reconstitute(
				new ImportBatchId(entity.getId()),
				new AccountId(entity.getAccountId()),
				entity.getFilename(),
				entity.getImportedAt(),
				entity.getAccepted(),
				entity.getDuplicate(),
				entity.getRejected());
	}

}
