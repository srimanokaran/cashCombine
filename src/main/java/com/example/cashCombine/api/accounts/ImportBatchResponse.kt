package com.example.cashCombine.api.accounts

import com.example.cashCombine.ledger.imports.ImportBatch
import java.time.Instant
import java.util.UUID

data class ImportBatchResponse(
    val id: UUID,
    val accountId: UUID,
    val filename: String?,
    val importedAt: Instant,
    val accepted: Int,
    val duplicate: Int,
    val rejected: Int,
) {

    companion object {
        fun from(batch: ImportBatch): ImportBatchResponse =
            ImportBatchResponse(
                batch.id.value,
                batch.accountId.value,
                batch.filename,
                batch.importedAt,
                batch.accepted,
                batch.duplicate,
                batch.rejected,
            )
    }
}