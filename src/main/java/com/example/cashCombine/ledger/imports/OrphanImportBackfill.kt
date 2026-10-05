package com.example.cashCombine.ledger.imports

import com.example.cashCombine.ledger.accounts.AccountId
import com.example.cashCombine.ledger.transactions.Transaction
import com.example.cashCombine.ledger.transactions.TransactionRepository
import java.time.Instant

object OrphanImportBackfill {

    const val LEGACY_FILENAME = "(imported before tracking)"

    @JvmStatic
    fun run(transactionRepository: TransactionRepository, importBatchRepository: ImportBatchRepository): Int {
        val orphansByAccount = LinkedHashMap<AccountId, MutableList<Transaction>>()
        for (transaction in transactionRepository.findAll()) {
            if (transaction.importBatchId() != null) {
                continue
            }
            orphansByAccount.getOrPut(transaction.accountId()) { ArrayList() }.add(transaction)
        }

        var accountsBackfilled = 0
        for ((accountId, orphans) in orphansByAccount) {
            val batchId = ImportBatchId.generate()
            val batch = ImportBatch.reconstitute(
                batchId,
                accountId,
                LEGACY_FILENAME,
                Instant.EPOCH,
                orphans.size,
                0,
                0
            )
            importBatchRepository.save(batch)

            for (orphan in orphans) {
                transactionRepository.save(orphan.withImportBatchId(batchId))
            }
            accountsBackfilled++
        }
        return accountsBackfilled
    }
}