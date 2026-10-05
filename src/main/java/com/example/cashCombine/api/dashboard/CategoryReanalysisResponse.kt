package com.example.cashCombine.api.dashboard

import com.example.cashCombine.ledger.categorisation.CategoryReanalysisResult

data class CategoryReanalysisResponse(val examined: Int, val updated: Int, val skippedManual: Int) {

    companion object {
        fun from(result: CategoryReanalysisResult): CategoryReanalysisResponse =
            CategoryReanalysisResponse(result.examined, result.updated, result.skippedManual)
    }
}