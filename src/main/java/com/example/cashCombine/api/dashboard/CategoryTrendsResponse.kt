package com.example.cashCombine.api.dashboard

import com.example.cashCombine.ledger.dashboard.CategoryTrendMonth
import com.example.cashCombine.ledger.dashboard.CategoryTrendReport
import com.example.cashCombine.ledger.dashboard.CategoryTrendSlice
import com.example.cashCombine.ledger.dashboard.CategoryTrendSummary
import java.math.BigDecimal
import java.util.UUID

data class CategoryTrendsResponse(
    val from: String?,
    val to: String?,
    val categories: List<CategoryTrendSummaryResponse>,
    val months: List<CategoryTrendMonthResponse>,
) {

    companion object {
        fun from(report: CategoryTrendReport): CategoryTrendsResponse =
            CategoryTrendsResponse(
                report.from?.toString(),
                report.to?.toString(),
                report.categories.map { CategoryTrendSummaryResponse.from(it) },
                report.months.map { CategoryTrendMonthResponse.from(it) },
            )
    }
}

data class CategoryTrendSummaryResponse(
    val categoryId: UUID,
    val categoryName: String,
    val total: BigDecimal,
) {

    companion object {
        fun from(summary: CategoryTrendSummary): CategoryTrendSummaryResponse =
            CategoryTrendSummaryResponse(
                summary.categoryId.value,
                summary.categoryName,
                summary.total,
            )
    }
}

data class CategoryTrendMonthResponse(
    val month: String,
    val totalSpend: BigDecimal,
    val slices: List<CategoryTrendSliceResponse>,
) {

    companion object {
        fun from(month: CategoryTrendMonth): CategoryTrendMonthResponse =
            CategoryTrendMonthResponse(
                month.month.toString(),
                month.totalSpend,
                month.slices.map { CategoryTrendSliceResponse.from(it) },
            )
    }
}

data class CategoryTrendSliceResponse(
    val categoryId: UUID,
    val amount: BigDecimal,
    val transactionCount: Int,
) {

    companion object {
        fun from(slice: CategoryTrendSlice): CategoryTrendSliceResponse =
            CategoryTrendSliceResponse(
                slice.categoryId.value,
                slice.amount,
                slice.transactionCount,
            )
    }
}