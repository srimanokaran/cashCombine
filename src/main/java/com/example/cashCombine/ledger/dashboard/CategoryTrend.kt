package com.example.cashCombine.ledger.dashboard

import com.example.cashCombine.ledger.categorisation.CategoryId
import java.math.BigDecimal
import java.time.YearMonth

/**
 * Spend per category over time.
 *
 * [categories] is the single source of truth for series metadata (name and range total), while
 * [CategoryTrendMonth.slices] only carries the per-month facts referenced by [CategoryTrendSlice.categoryId].
 */
data class CategoryTrendReport(
    @get:JvmName("from") val from: YearMonth?,
    @get:JvmName("to") val to: YearMonth?,
    @get:JvmName("categories") val categories: List<CategoryTrendSummary>,
    @get:JvmName("months") val months: List<CategoryTrendMonth>)

data class CategoryTrendSummary(
    @get:JvmName("categoryId") val categoryId: CategoryId,
    @get:JvmName("categoryName") val categoryName: String,
    @get:JvmName("total") val total: BigDecimal)

data class CategoryTrendMonth(
    @get:JvmName("month") val month: YearMonth,
    @get:JvmName("totalSpend") val totalSpend: BigDecimal,
    @get:JvmName("slices") val slices: List<CategoryTrendSlice>)

data class CategoryTrendSlice(
    @get:JvmName("categoryId") val categoryId: CategoryId,
    @get:JvmName("amount") val amount: BigDecimal,
    @get:JvmName("transactionCount") val transactionCount: Int)