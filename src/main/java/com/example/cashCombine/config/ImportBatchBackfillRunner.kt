package com.example.cashCombine.config

import com.example.cashCombine.ledger.imports.ImportBatchRepository
import com.example.cashCombine.ledger.imports.OrphanImportBackfill
import com.example.cashCombine.ledger.transactions.TransactionRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Order(2)
open class ImportBatchBackfillRunner(
    private val transactionRepository: TransactionRepository,
    private val importBatchRepository: ImportBatchRepository,
) : ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments) {
        val accounts = OrphanImportBackfill.run(transactionRepository, importBatchRepository)
        if (accounts > 0) {
            log.info("Backfilled legacy import batches for {} account(s)", accounts)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger("api")
    }
}