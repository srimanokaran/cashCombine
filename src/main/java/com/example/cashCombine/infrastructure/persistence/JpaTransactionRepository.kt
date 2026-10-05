package com.example.cashCombine.infrastructure.persistence

import com.example.cashCombine.ledger.accounts.AccountId
import com.example.cashCombine.ledger.categorisation.CategoryId
import com.example.cashCombine.ledger.imports.ImportBatchId
import com.example.cashCombine.ledger.transactions.Transaction
import com.example.cashCombine.ledger.transactions.TransactionFingerprint
import com.example.cashCombine.ledger.transactions.TransactionId
import com.example.cashCombine.ledger.transactions.TransactionRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaTransactionRepository(private val jpaRepository: TransactionJpaRepository) : TransactionRepository {

    override fun save(transaction: Transaction): Transaction {
        val entity = TransactionJpaEntity(
            transaction.id().value,
            transaction.accountId().value,
            transaction.importBatchId()?.value,
            transaction.date(),
            transaction.amount(),
            transaction.description(),
            transaction.balance(),
            transaction.categoryId().value,
            transaction.categoryAssignmentSource())
        jpaRepository.save(entity)
        return transaction
    }

    @Transactional(readOnly = true)
    override fun findById(id: TransactionId): Transaction? {
        return jpaRepository.findById(id.value).map { toDomain(it) }.orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByAccountId(accountId: AccountId): List<Transaction> {
        return jpaRepository.findByAccountId(accountId.value).map { toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findByCategoryId(categoryId: CategoryId): List<Transaction> {
        return jpaRepository.findByCategoryId(categoryId.value).map { toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<Transaction> {
        return jpaRepository.findAll().map { toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun existsByAccountAndFingerprint(accountId: AccountId, fingerprint: TransactionFingerprint): Boolean {
        return jpaRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndBalance(
            accountId.value,
            fingerprint.date,
            fingerprint.amount,
            fingerprint.description,
            fingerprint.balance)
    }

    @Transactional(readOnly = true)
    override fun existsByAccountId(accountId: AccountId): Boolean {
        return jpaRepository.existsByAccountId(accountId.value)
    }

    override fun deleteByAccountId(accountId: AccountId) {
        jpaRepository.deleteByAccountId(accountId.value)
    }

    override fun deleteByImportBatchId(importBatchId: ImportBatchId) {
        jpaRepository.deleteByImportBatchId(importBatchId.value)
    }

    private fun toDomain(entity: TransactionJpaEntity): Transaction {
        val importBatchId = entity.importBatchId?.let { ImportBatchId(it) }
        return Transaction.reconstitute(
            TransactionId(entity.id!!),
            AccountId(entity.accountId!!),
            importBatchId,
            entity.date!!,
            entity.amount!!,
            entity.description!!,
            entity.balance!!,
            CategoryId(entity.categoryId!!),
            entity.categoryAssignmentSource!!)
    }
}