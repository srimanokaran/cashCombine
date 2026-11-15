package com.example.cashCombine.ledger.search

import com.example.cashCombine.ledger.accounts.AccountId
import com.example.cashCombine.ledger.categorisation.CategoryAssignmentSource
import com.example.cashCombine.ledger.categorisation.CategoryId
import com.example.cashCombine.ledger.transactions.TransactionId
import java.math.BigDecimal
import java.time.LocalDate

enum class SearchMatch {
    /** Matched the terms the user typed. */
    QUERY,

    /** Matched only a related term suggested by the query interpreter. */
    EXPANDED,

    /** Selected by the local model rather than by keyword matching. */
    AI
}

data class SearchTransaction(
    @get:JvmName("id") val id: TransactionId,
    @get:JvmName("accountId") val accountId: AccountId,
    @get:JvmName("accountName") val accountName: String,
    @get:JvmName("date") val date: LocalDate,
    @get:JvmName("amount") val amount: BigDecimal,
    @get:JvmName("description") val description: String,
    @get:JvmName("categoryId") val categoryId: CategoryId,
    @get:JvmName("categoryName") val categoryName: String,
    @get:JvmName("categoryAssignmentSource") val categoryAssignmentSource: CategoryAssignmentSource,
    @get:JvmName("matchedBy") val matchedBy: SearchMatch)

data class SearchSuggestion(
    @get:JvmName("label") val label: String,
    @get:JvmName("matchCount") val matchCount: Int,
    @get:JvmName("total") val total: BigDecimal,
    @get:JvmName("categoryName") val categoryName: String)

data class SearchCategorySplit(
    @get:JvmName("categoryId") val categoryId: CategoryId,
    @get:JvmName("categoryName") val categoryName: String,
    @get:JvmName("amount") val amount: BigDecimal,
    @get:JvmName("transactionCount") val transactionCount: Int)

data class ExpenseSearchResult(
    @get:JvmName("query") val query: String,
    /** Net spend for the matched expenses; refunds reduce it and a net refund stays negative. */
    @get:JvmName("totalSpend") val totalSpend: BigDecimal,
    @get:JvmName("expenseCount") val expenseCount: Int,
    /** Matches that are income or funds between accounts and therefore never count as spend. */
    @get:JvmName("excludedCount") val excludedCount: Int,
    @get:JvmName("expandedTerms") val expandedTerms: List<String>,
    @get:JvmName("firstDate") val firstDate: LocalDate?,
    @get:JvmName("lastDate") val lastDate: LocalDate?,
    @get:JvmName("categories") val categories: List<SearchCategorySplit>,
    @get:JvmName("suggestions") val suggestions: List<SearchSuggestion>,
    @get:JvmName("transactions") val transactions: List<SearchTransaction>,
    /** True when [transactions] was capped by the limit. */
    @get:JvmName("truncated") val truncated: Boolean)
