package com.example.cashCombine.ledger.imports

import com.example.cashCombine.ledger.accounts.Account
import com.example.cashCombine.ledger.accounts.AccountId
import com.example.cashCombine.ledger.accounts.AccountNotFoundException
import com.example.cashCombine.ledger.accounts.AccountRepository
import com.example.cashCombine.ledger.accounts.AccountType
import com.example.cashCombine.ledger.categorisation.CategoryId
import com.example.cashCombine.ledger.categorisation.TransactionClassifier
import com.example.cashCombine.ledger.transactions.Transaction
import com.example.cashCombine.ledger.transactions.TransactionFingerprint
import com.example.cashCombine.ledger.transactions.TransactionFingerprintStrategy
import com.example.cashCombine.ledger.transactions.TransactionRepository
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional

open class ImportService(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val importBatchRepository: ImportBatchRepository,
    private val parsers: Map<AccountType, TransactionCsvParser>,
    private val fingerprintStrategies: Map<AccountType, TransactionFingerprintStrategy>,
    private val classifier: TransactionClassifier
) {

    /**
     * Imports CSV transactions into an account (idempotent).
     *
     * Input:
     * - accountId: target account (must exist; type selects parser and fingerprint rules)
     * - input: CSV bytes (format depends on account type)
     * - filename: optional original upload name (stored on the import batch)
     *
     * Output ImportResult:
     * - id: import batch id (transactions created by this upload are linked to it)
     * - accepted: new rows saved (with category from rules or Uncategorised)
     * - duplicate: already seen for this account (skipped, not updated)
     * - rejected: bad rows after the first data row (skipped; import continues)
     *
     * Failures:
     * - missing account: AccountNotFoundException
     * - first data row unparseable: InvalidCsvFormatException (whole import fails)
     */
    open fun importCsv(accountId: AccountId, input: InputStream): ImportResult =
        importCsv(accountId, input, null)

    open fun importCsv(accountId: AccountId, input: InputStream, filename: String?): ImportResult {
        val account = requireAccount(accountId)
        val parser = requireParser(account.type())
        val fingerprintStrategy = requireFingerprintStrategy(account.type())

        val batchId = ImportBatchId.generate()
        val result = processRows(accountId, batchId, input, parser, fingerprintStrategy)
        importBatchRepository.save(
            ImportBatch.create(batchId, accountId, filename, result.accepted, result.duplicate, result.rejected)
        )
        markImportedIfNeeded(account, result)
        return result
    }

    @Transactional(readOnly = true)
    open fun listImports(accountId: AccountId): List<ImportBatch> {
        requireAccount(accountId)
        return importBatchRepository.findByAccountId(accountId)
    }

    @Transactional
    open fun deleteImport(accountId: AccountId, importBatchId: ImportBatchId) {
        val account = requireAccount(accountId)
        val batch = importBatchRepository
            .findById(importBatchId)
            ?: throw ImportNotFoundException(importBatchId)
        if (batch.accountId != accountId) {
            throw ImportNotFoundException(importBatchId)
        }

        transactionRepository.deleteByImportBatchId(importBatchId)
        importBatchRepository.deleteById(importBatchId)

        if (!transactionRepository.existsByAccountId(accountId)) {
            account.clearImports()
            accountRepository.save(account)
        }
    }

    private fun requireAccount(accountId: AccountId): Account =
        accountRepository.findById(accountId) ?: throw AccountNotFoundException(accountId)

    private fun requireParser(accountType: AccountType): TransactionCsvParser =
        parsers[accountType]
            ?: throw IllegalArgumentException("No CSV parser registered for account type: $accountType")

    private fun requireFingerprintStrategy(accountType: AccountType): TransactionFingerprintStrategy =
        fingerprintStrategies[accountType]
            ?: throw IllegalArgumentException("No fingerprint strategy registered for account type: $accountType")

    private fun processRows(
        accountId: AccountId,
        batchId: ImportBatchId,
        input: InputStream,
        parser: TransactionCsvParser,
        fingerprintStrategy: TransactionFingerprintStrategy
    ): ImportResult {
        var accepted = 0
        var duplicate = 0
        var rejected = 0
        val acceptedThisImport = HashSet<TransactionFingerprint>()
        var firstDataRow = true

        BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                if (line.isBlank() || parser.shouldSkipLine(line)) {
                    line = reader.readLine()
                    continue
                }

                val outcome =
                    processRow(accountId, batchId, line, parser, fingerprintStrategy, acceptedThisImport, firstDataRow)
                when (outcome) {
                    RowOutcome.ACCEPTED -> accepted++
                    RowOutcome.DUPLICATE -> duplicate++
                    RowOutcome.REJECTED -> rejected++
                }
                firstDataRow = false
                line = reader.readLine()
            }
        }

        return ImportResult(batchId, accepted, duplicate, rejected)
    }

    private fun processRow(
        accountId: AccountId,
        batchId: ImportBatchId,
        line: String,
        parser: TransactionCsvParser,
        fingerprintStrategy: TransactionFingerprintStrategy,
        acceptedThisImport: MutableSet<TransactionFingerprint>,
        firstDataRow: Boolean
    ): RowOutcome {
        return try {
            val row = parser.parseLine(line)
            val fingerprint = fingerprintStrategy.fingerprint(row)

            if (transactionRepository.existsByAccountAndFingerprint(accountId, fingerprint)
                || acceptedThisImport.contains(fingerprint)
            ) {
                return RowOutcome.DUPLICATE
            }

            val categoryId = classifier.classify(row.description)
            try {
                transactionRepository.save(Transaction.create(accountId, row, categoryId, batchId))
            } catch (ex: DataIntegrityViolationException) {
                return RowOutcome.DUPLICATE
            }
            acceptedThisImport.add(fingerprint)
            RowOutcome.ACCEPTED
        } catch (ex: InvalidCsvRowException) {
            if (firstDataRow) {
                throw InvalidCsvFormatException(ex.message!!, ex)
            }
            RowOutcome.REJECTED
        }
    }

    private fun markImportedIfNeeded(account: Account, result: ImportResult) {
        if (result.accepted > 0 || result.duplicate > 0) {
            account.markAsImported()
            accountRepository.save(account)
        }
    }

    private enum class RowOutcome {
        ACCEPTED,
        DUPLICATE,
        REJECTED
    }
}