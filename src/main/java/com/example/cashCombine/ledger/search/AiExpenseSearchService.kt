package com.example.cashCombine.ledger.search

import com.example.cashCombine.ledger.categorisation.CategoryRepository
import com.example.cashCombine.ledger.transactions.TransactionId
import com.example.cashCombine.ledger.transactions.TransactionRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

/** Public errors deliberately never contain prompts, bank data or errors returned by Ollama. */
class AiSearchException(val status: HttpStatus, message: String) : RuntimeException(message) {
    companion object {
        fun unavailable() = AiSearchException(HttpStatus.SERVICE_UNAVAILABLE,
            "Local AI search is unavailable or returned an invalid response. Please try again.")
        fun tooLarge(message: String) = AiSearchException(HttpStatus.UNPROCESSABLE_ENTITY, message)
    }
}

data class AiSearchRow(
    val id: String,
    val date: String,
    val amount: String,
    val description: String,
    val category: String,
)

interface AiTransactionSelector {
    fun select(query: String, rows: List<AiSearchRow>): Set<String>
}

@Service
class AiExpenseSearchService(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val expenseSearchService: ExpenseSearchService,
    private val selector: AiTransactionSelector,
    private val runtime: LocalOllamaRuntime,
) {
    fun start() {
        try {
            runtime.ensureAvailable()
        } catch (ex: LocalOllamaException) {
            when (ex.reason) {
                LocalOllamaException.Reason.BINARY_NOT_FOUND -> throw AiSearchException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Ollama is not installed or could not be found.")
                LocalOllamaException.Reason.MODEL_MISSING -> throw AiSearchException(
                    HttpStatus.SERVICE_UNAVAILABLE, "The local llama3 model is not installed. Install it with ollama pull llama3.")
                else -> throw AiSearchException.unavailable()
            }
        } catch (_: Exception) {
            throw AiSearchException.unavailable()
        }
    }

    fun search(query: String): ExpenseSearchResult {
        val trimmed = query.trim()
        require(trimmed.isNotEmpty() && trimmed.length <= MAX_QUERY_LENGTH &&
            trimmed.toByteArray(Charsets.UTF_8).size <= MAX_QUERY_BYTES) {
            "Enter an AI search query of at most $MAX_QUERY_LENGTH characters."
        }
        // Account-like numbers must not be sent even if supplied in a query.
        require(!ACCOUNT_NUMBER.containsMatchIn(trimmed)) {
            "AI search queries cannot contain account-like numbers."
        }
        val transactions = transactionRepository.findUpTo(MAX_TRANSACTIONS + 1)
        if (transactions.size > MAX_TRANSACTIONS) {
            throw AiSearchException.tooLarge("Too many transactions for AI search. Reduce the ledger size and try again.")
        }
        val categories = categoryRepository.findAll().associateBy { it.id }
        val rows = transactions.map { transaction ->
            val category = categories[transaction.categoryId()] ?: throw AiSearchException.unavailable()
            AiSearchRow(
                transaction.id().value.toString(), transaction.date().toString(),
                transaction.amount().toPlainString(),
                redactAccountNumbers(transaction.description()), redactAccountNumbers(category.name),
            )
        }
        start()
        val selected = selector.select(trimmed, rows)
        // Validate even an alternate selector implementation before aggregating the snapshot.
        val ids = transactions.associate { it.id().value.toString() to it.id() }
        if (!ids.keys.containsAll(selected)) throw AiSearchException.unavailable()
        val selectedIds: Set<TransactionId> = selected.map { ids.getValue(it) }.toSet()
        return expenseSearchService.searchSelected(trimmed, transactions, categories, selectedIds)
    }

    companion object {
        const val MAX_QUERY_LENGTH = 500
        const val MAX_QUERY_BYTES = 1000
        const val MAX_TRANSACTIONS = 1000
        // Descriptions can contain payment references or account numbers; don't send long digit runs.
        private val ACCOUNT_NUMBER = Regex("(?<![0-9])(?:[0-9][ -]?){5,}[0-9](?![0-9])")
        private fun redactAccountNumbers(value: String) = ACCOUNT_NUMBER.replace(value, "[number redacted]")
    }
}
