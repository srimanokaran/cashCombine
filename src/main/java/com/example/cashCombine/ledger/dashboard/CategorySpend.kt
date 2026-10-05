package com.example.cashCombine.ledger.dashboard

import com.example.cashCombine.ledger.categorisation.CategoryId
import java.math.BigDecimal

data class CategorySpend(
    @get:JvmName("categoryId") val categoryId: CategoryId,
    @get:JvmName("categoryName") val categoryName: String,
    @get:JvmName("amount") val amount: BigDecimal,
    @get:JvmName("percent") val percent: BigDecimal,
    @get:JvmName("transactionCount") val transactionCount: Int)