package com.example.cashCombine.ledger.imports

import com.example.cashCombine.ledger.accounts.AccountId

interface ImportBatchRepository {

    fun save(importBatch: ImportBatch): ImportBatch

    fun findById(id: ImportBatchId): ImportBatch?

    fun findByAccountId(accountId: AccountId): List<ImportBatch>

    fun deleteById(id: ImportBatchId)

    fun deleteByAccountId(accountId: AccountId)
}