package com.example.cashCombine.api.accounts

import com.example.cashCombine.ledger.imports.ImportResult
import java.util.UUID

data class ImportResultResponse(val id: UUID, val accepted: Int, val duplicate: Int, val rejected: Int) {

    companion object {
        fun from(result: ImportResult): ImportResultResponse =
            ImportResultResponse(result.id.value, result.accepted, result.duplicate, result.rejected)
    }
}