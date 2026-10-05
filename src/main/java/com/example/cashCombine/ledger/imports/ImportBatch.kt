package com.example.cashCombine.ledger.imports

import com.example.cashCombine.ledger.accounts.AccountId
import java.time.Instant

class ImportBatch private constructor(
    @get:JvmName("id") val id: ImportBatchId,
    @get:JvmName("accountId") val accountId: AccountId,
    @get:JvmName("filename") val filename: String?,
    @get:JvmName("importedAt") val importedAt: Instant,
    @get:JvmName("accepted") val accepted: Int,
    @get:JvmName("duplicate") val duplicate: Int,
    @get:JvmName("rejected") val rejected: Int
) {

    companion object {
        @JvmStatic
        fun create(
            id: ImportBatchId,
            accountId: AccountId,
            filename: String?,
            accepted: Int,
            duplicate: Int,
            rejected: Int
        ): ImportBatch {
            require(id != null) { "Import batch id is required" }
            require(accountId != null) { "Account id is required" }
            val normalisedFilename: String? =
                if (filename.isNullOrBlank()) null else filename.trim()
            return ImportBatch(id, accountId, normalisedFilename, Instant.now(), accepted, duplicate, rejected)
        }

        @JvmStatic
        fun reconstitute(
            id: ImportBatchId,
            accountId: AccountId,
            filename: String?,
            importedAt: Instant,
            accepted: Int,
            duplicate: Int,
            rejected: Int
        ): ImportBatch =
            ImportBatch(id, accountId, filename, importedAt, accepted, duplicate, rejected)
    }
}