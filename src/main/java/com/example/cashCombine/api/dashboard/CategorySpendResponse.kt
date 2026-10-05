package com.example.cashCombine.api.dashboard

import com.example.cashCombine.ledger.dashboard.CategorySpend
import java.math.BigDecimal
import java.util.UUID

data class CategorySpendResponse(
    val categoryId: UUID,
    val categoryName: String,
    val amount: BigDecimal,
    val percent: BigDecimal,
    val transactionCount: Int,
) {

    companion object {
        fun from(spend: CategorySpend): CategorySpendResponse =
            CategorySpendResponse(
                spend.categoryId.value,
                spend.categoryName,
                spend.amount,
                spend.percent,
                spend.transactionCount,
            )
    }
}