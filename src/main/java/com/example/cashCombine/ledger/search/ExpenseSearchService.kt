package com.example.cashCombine.ledger.search

import com.example.cashCombine.ledger.accounts.AccountId
import com.example.cashCombine.ledger.accounts.AccountRepository
import com.example.cashCombine.ledger.categorisation.Category
import com.example.cashCombine.ledger.categorisation.CategoryId
import com.example.cashCombine.ledger.categorisation.CategoryRepository
import com.example.cashCombine.ledger.categorisation.RulePattern
import com.example.cashCombine.ledger.dashboard.DateWindow
import com.example.cashCombine.ledger.dashboard.SpendClassifier
import com.example.cashCombine.ledger.dashboard.SpendKind
import com.example.cashCombine.ledger.transactions.Transaction
import com.example.cashCombine.ledger.transactions.TransactionId
import com.example.cashCombine.ledger.transactions.TransactionRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import org.springframework.transaction.annotation.Transactional

/**
 * Deterministic expense search: a query term must appear in the bank description or in the
 * category name. Totals are computed here, never by a model — an optional [ExpenseQueryInterpreter]
 * can only add related terms, which are matched with OR against the original AND-terms.
 */
@Transactional(readOnly = true)
open class ExpenseSearchService(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val queryInterpreter: ExpenseQueryInterpreter = NoopExpenseQueryInterpreter(),
) {

    fun search(
        query: String,
        window: DateWindow = DateWindow.ALL,
        expandedTerms: List<String> = emptyList(),
        limit: Int = DEFAULT_LIMIT,
    ): ExpenseSearchResult {
        val trimmedQuery = query.trim()
        val queryTerms = SearchTokens.parse(trimmedQuery)
        val extraTerms = SearchTokens.sanitise(expandedTerms)
            .filterNot { queryTerms.contains(it) }
        if (queryTerms.isEmpty()) {
            return emptyResult(trimmedQuery, extraTerms)
        }

        return aggregate(
            trimmedQuery, extraTerms, transactionRepository.findAll(), categoriesById(),
            limit.coerceIn(1, MAX_LIMIT),
        ) { transaction, category ->
            if (!window.contains(transaction.date())) return@aggregate null
            val haystack = haystack(transaction, category)
            when {
                queryTerms.all { haystack.contains(it) } -> SearchMatch.QUERY
                extraTerms.any { haystack.contains(it) } -> SearchMatch.EXPANDED
                else -> null
            }
        }
    }

    /** Aggregate exactly the snapshot selected by the model, using the same spend rules as search. */
    fun searchSelected(
        query: String,
        transactions: List<Transaction>,
        categories: Map<CategoryId, Category>,
        selectedIds: Set<TransactionId>,
    ): ExpenseSearchResult {
        if (selectedIds.size > MAX_LIMIT) {
            throw AiSearchException.tooLarge("AI search matched too many transactions. Please narrow your query.")
        }
        return aggregate(query, emptyList(), transactions, categories, MAX_LIMIT) { transaction, _ ->
            if (transaction.id() in selectedIds) SearchMatch.AI else null
        }
    }

    private fun aggregate(
        query: String,
        extraTerms: List<String>,
        transactions: List<Transaction>,
        categoriesById: Map<CategoryId, Category>,
        cap: Int,
        match: (Transaction, Category) -> SearchMatch?,
    ): ExpenseSearchResult {
        val accountNames = accountNames()
        val rows = mutableListOf<SearchTransaction>()
        val categoryBuckets = mutableMapOf<CategoryId, CategoryBucket>()
        val suggestionBuckets = mutableMapOf<String, SuggestionBucket>()
        var signedTotal = BigDecimal.ZERO
        var excludedCount = 0
        var firstDate: LocalDate? = null
        var lastDate: LocalDate? = null

        for (transaction in transactions) {
            val category = categoriesById[transaction.categoryId()] ?: continue
            val matchedBy = match(transaction, category) ?: continue
            if (SpendClassifier.classify(category, transaction.amount()) != SpendKind.EXPENSE) {
                excludedCount++
                continue
            }

            rows.add(SearchTransaction(
                transaction.id(),
                transaction.accountId(),
                accountNames.getOrDefault(transaction.accountId(), UNKNOWN_ACCOUNT),
                transaction.date(),
                transaction.amount().setScale(2, RoundingMode.HALF_UP),
                transaction.description(),
                category.id,
                category.name,
                transaction.categoryAssignmentSource(),
                matchedBy,
            ))
            signedTotal = signedTotal.add(transaction.amount())

            val categoryBucket = categoryBuckets.computeIfAbsent(category.id) {
                CategoryBucket(category.name)
            }
            categoryBucket.net = categoryBucket.net.add(transaction.amount())
            categoryBucket.count++

            val label = suggestionLabel(transaction.description())
            val suggestionBucket = suggestionBuckets.computeIfAbsent(label.lowercase()) {
                SuggestionBucket(label)
            }
            suggestionBucket.net = suggestionBucket.net.add(transaction.amount())
            suggestionBucket.count++
            suggestionBucket.categories.merge(category.name, 1) { a, b -> a + b }

            val date = transaction.date()
            val earliest = firstDate
            if (earliest == null || date.isBefore(earliest)) {
                firstDate = date
            }
            val latest = lastDate
            if (latest == null || date.isAfter(latest)) {
                lastDate = date
            }
        }

        rows.sortWith(compareByDescending<SearchTransaction> { it.date }
            .thenBy { it.description })

        return ExpenseSearchResult(
            query,
            netSpend(signedTotal),
            rows.size,
            excludedCount,
            extraTerms,
            firstDate,
            lastDate,
            categorySplits(categoryBuckets),
            suggestions(suggestionBuckets),
            rows.take(cap),
            rows.size > cap,
        )
    }

    /**
     * Asks the interpreter for related terms. Returns `available = false` when the interpreter is
     * disabled, unreachable, too slow or produced nothing usable — the caller simply keeps the
     * deterministic result.
     */
    fun expand(query: String): ExpenseQueryExpansion {
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < MIN_EXPAND_QUERY_LENGTH) {
            return ExpenseQueryExpansion(false, trimmedQuery, emptyList())
        }
        val rawTerms = queryInterpreter.expand(trimmedQuery)
            ?: return ExpenseQueryExpansion(false, trimmedQuery, emptyList())
        val queryTerms = SearchTokens.parse(trimmedQuery)
        val terms = SearchTokens.sanitise(rawTerms).filterNot { queryTerms.contains(it) }
        return ExpenseQueryExpansion(true, trimmedQuery, terms)
    }

    private fun haystack(transaction: Transaction, category: Category): String =
        "${transaction.description()} ${category.name}".lowercase()

    private fun suggestionLabel(description: String): String {
        val pattern = if (description.isBlank()) {
            UNKNOWN_MERCHANT
        } else {
            RulePattern.fromDescription(description)
        }
        // Merge "WOOLWORTHS 1234" with "WOOLWORTHS 5678": the same rule normalisation the
        // classifier uses, minus trailing reference numbers.
        val withoutReferences = pattern.replace(TRAILING_REFERENCES, "").trim()
        val label = if (withoutReferences.isBlank()) pattern else withoutReferences
        return label.replace(WHITESPACE, " ").trim()
    }

    private fun categorySplits(buckets: Map<CategoryId, CategoryBucket>): List<SearchCategorySplit> =
        buckets.map { (categoryId, bucket) ->
            SearchCategorySplit(
                categoryId,
                bucket.name,
                netSpend(bucket.net),
                bucket.count,
            )
        }.sortedWith(compareByDescending<SearchCategorySplit> { it.amount }
            .thenBy { it.categoryName })

    private fun suggestions(buckets: Map<String, SuggestionBucket>): List<SearchSuggestion> =
        buckets.values.map { bucket ->
            SearchSuggestion(
                bucket.label,
                bucket.count,
                netSpend(bucket.net),
                bucket.categories.maxByOrNull { it.value }?.key ?: UNKNOWN_CATEGORY,
            )
        }.sortedWith(compareByDescending<SearchSuggestion> { it.total }
            .thenByDescending { it.matchCount }
            .thenBy { it.label })
            .take(MAX_SUGGESTIONS)

    private fun netSpend(signedTotal: BigDecimal): BigDecimal =
        signedTotal.negate().setScale(2, RoundingMode.HALF_UP)

    private fun emptyResult(query: String, expandedTerms: List<String>): ExpenseSearchResult =
        ExpenseSearchResult(
            query,
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
            0,
            0,
            expandedTerms,
            null,
            null,
            emptyList(),
            emptyList(),
            emptyList(),
            false,
        )

    private fun categoriesById(): Map<CategoryId, Category> {
        val categoriesById = mutableMapOf<CategoryId, Category>()
        for (category in categoryRepository.findAll()) {
            categoriesById[category.id] = category
        }
        return categoriesById
    }

    private fun accountNames(): Map<AccountId, String> {
        val accountNames = mutableMapOf<AccountId, String>()
        for (account in accountRepository.findAll()) {
            accountNames[account.id()] = account.name()
        }
        return accountNames
    }

    private class CategoryBucket(val name: String) {
        var net: BigDecimal = BigDecimal.ZERO
        var count: Int = 0
    }

    private class SuggestionBucket(val label: String) {
        var net: BigDecimal = BigDecimal.ZERO
        var count: Int = 0
        val categories = mutableMapOf<String, Int>()
    }

    companion object {
        const val DEFAULT_LIMIT = 200
        const val MAX_LIMIT = 500
        const val MAX_SUGGESTIONS = 8
        const val MIN_EXPAND_QUERY_LENGTH = 3

        private const val UNKNOWN_ACCOUNT = "Unknown"
        private const val UNKNOWN_CATEGORY = "Unknown"
        private const val UNKNOWN_MERCHANT = "Unknown merchant"
        private val WHITESPACE = Regex("\\s+")
        private val TRAILING_REFERENCES = Regex("(\\s+\\d+)+$")
    }
}
