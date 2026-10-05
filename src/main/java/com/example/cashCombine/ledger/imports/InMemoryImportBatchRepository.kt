package com.example.cashCombine.ledger.imports

import com.example.cashCombine.ledger.accounts.AccountId
import java.util.Comparator

class InMemoryImportBatchRepository : ImportBatchRepository {

    private val batches = HashMap<ImportBatchId, ImportBatch>()

    override fun save(importBatch: ImportBatch): ImportBatch {
        batches[importBatch.id] = importBatch
        return importBatch
    }

    override fun findById(id: ImportBatchId): ImportBatch? = batches[id]

    override fun findByAccountId(accountId: AccountId): List<ImportBatch> =
        batches.values
            .filter { it.accountId == accountId }
            .sortedByDescending { it.importedAt }

    override fun deleteById(id: ImportBatchId) {
        batches.remove(id)
    }

    override fun deleteByAccountId(accountId: AccountId) {
        batches.entries.removeIf { it.value.accountId == accountId }
    }
}