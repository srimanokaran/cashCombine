package com.example.cashCombine.ledger.transactions

import com.example.cashCombine.ledger.accounts.AccountId
import com.example.cashCombine.ledger.categorisation.CategoryId
import com.example.cashCombine.ledger.imports.ImportBatchId

interface TransactionRepository {

    fun save(transaction: Transaction): Transaction

    fun findById(id: TransactionId): Transaction?

    fun findByAccountId(accountId: AccountId): List<Transaction>

    fun findByCategoryId(categoryId: CategoryId): List<Transaction>

    fun findAll(): List<Transaction>

    fun existsByAccountAndFingerprint(accountId: AccountId, fingerprint: TransactionFingerprint): Boolean

    fun existsByAccountId(accountId: AccountId): Boolean

    fun deleteByAccountId(accountId: AccountId)

    fun deleteByImportBatchId(importBatchId: ImportBatchId)
}