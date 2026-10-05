package com.example.cashCombine.infrastructure.persistence

import com.example.cashCombine.ledger.accounts.AccountId
import com.example.cashCombine.ledger.imports.ImportBatch
import com.example.cashCombine.ledger.imports.ImportBatchId
import com.example.cashCombine.ledger.imports.ImportBatchRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaImportBatchRepository(private val jpaRepository: ImportBatchJpaRepository) : ImportBatchRepository {

    override fun save(importBatch: ImportBatch): ImportBatch {
        val entity = ImportBatchJpaEntity(
            importBatch.id.value,
            importBatch.accountId.value,
            importBatch.filename,
            importBatch.importedAt,
            importBatch.accepted,
            importBatch.duplicate,
            importBatch.rejected)
        jpaRepository.save(entity)
        return importBatch
    }

    @Transactional(readOnly = true)
    override fun findById(id: ImportBatchId): ImportBatch? {
        return jpaRepository.findById(id.value).map { toDomain(it) }.orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByAccountId(accountId: AccountId): List<ImportBatch> {
        return jpaRepository.findByAccountIdOrderByImportedAtDesc(accountId.value)
            .map { toDomain(it) }
    }

    override fun deleteById(id: ImportBatchId) {
        jpaRepository.deleteById(id.value)
    }

    override fun deleteByAccountId(accountId: AccountId) {
        jpaRepository.deleteByAccountId(accountId.value)
    }

    private fun toDomain(entity: ImportBatchJpaEntity): ImportBatch {
        return ImportBatch.reconstitute(
            ImportBatchId(entity.id!!),
            AccountId(entity.accountId!!),
            entity.filename,
            entity.importedAt!!,
            entity.accepted,
            entity.duplicate,
            entity.rejected)
    }
}