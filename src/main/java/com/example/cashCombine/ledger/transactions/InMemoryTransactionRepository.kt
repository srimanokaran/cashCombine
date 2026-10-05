package com.example.cashCombine.ledger.transactions

import com.example.cashCombine.ledger.accounts.AccountId
import com.example.cashCombine.ledger.categorisation.CategoryId
import com.example.cashCombine.ledger.imports.ImportBatchId
import org.springframework.dao.DataIntegrityViolationException

class InMemoryTransactionRepository : TransactionRepository {

    private data class AccountFingerprintKey(val accountId: AccountId, val fingerprint: TransactionFingerprint)

    private val transactions = mutableMapOf<TransactionId, Transaction>()
    private val fingerprints = mutableSetOf<AccountFingerprintKey>()

    override fun save(transaction: Transaction): Transaction {
        val key = AccountFingerprintKey(transaction.accountId(), transaction.fingerprint())
        val sameRow = transactions.containsKey(transaction.id())
        if (!sameRow && fingerprints.contains(key)) {
            throw DataIntegrityViolationException("Duplicate transaction fingerprint")
        }
        transactions[transaction.id()] = transaction
        fingerprints.add(key)
        return transaction
    }

    override fun findById(id: TransactionId): Transaction? = transactions[id]

    override fun findByAccountId(accountId: AccountId): List<Transaction> =
        transactions.values.filter { it.accountId() == accountId }

    override fun findByCategoryId(categoryId: CategoryId): List<Transaction> =
        transactions.values.filter { it.categoryId() == categoryId }

    override fun findAll(): List<Transaction> = transactions.values.toList()

    override fun existsByAccountAndFingerprint(accountId: AccountId, fingerprint: TransactionFingerprint): Boolean =
        fingerprints.contains(AccountFingerprintKey(accountId, fingerprint))

    override fun existsByAccountId(accountId: AccountId): Boolean =
        transactions.values.any { it.accountId() == accountId }

    override fun deleteByAccountId(accountId: AccountId) {
        transactions.entries.removeIf { it.value.accountId() == accountId }
        fingerprints.removeIf { it.accountId == accountId }
    }

    override fun deleteByImportBatchId(importBatchId: ImportBatchId) {
        val toRemove = transactions.values.filter { importBatchId == it.importBatchId() }
        for (transaction in toRemove) {
            transactions.remove(transaction.id())
            fingerprints.remove(AccountFingerprintKey(transaction.accountId(), transaction.fingerprint()))
        }
    }
}