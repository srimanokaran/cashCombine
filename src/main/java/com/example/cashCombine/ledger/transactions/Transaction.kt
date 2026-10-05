package com.example.cashCombine.ledger.transactions

import com.example.cashCombine.ledger.accounts.AccountId
import com.example.cashCombine.ledger.categorisation.CategoryAssignmentSource
import com.example.cashCombine.ledger.categorisation.CategoryId
import com.example.cashCombine.ledger.imports.ImportBatchId
import com.example.cashCombine.ledger.imports.ParsedTransactionRow
import java.math.BigDecimal
import java.time.LocalDate

class Transaction private constructor(
    private val _id: TransactionId,
    private val _accountId: AccountId,
    private val _importBatchId: ImportBatchId?,
    private val _date: LocalDate,
    private val _amount: BigDecimal,
    private val _description: String,
    private val _balance: BigDecimal,
    private var _categoryId: CategoryId,
    private var _categoryAssignmentSource: CategoryAssignmentSource,
) {

    fun id(): TransactionId = _id

    fun accountId(): AccountId = _accountId

    fun importBatchId(): ImportBatchId? = _importBatchId

    fun date(): LocalDate = _date

    fun amount(): BigDecimal = _amount

    fun description(): String = _description

    fun balance(): BigDecimal = _balance

    fun categoryId(): CategoryId = _categoryId

    fun categoryAssignmentSource(): CategoryAssignmentSource = _categoryAssignmentSource

    fun isManuallyCategorised(): Boolean = _categoryAssignmentSource == CategoryAssignmentSource.MANUAL

    fun fingerprint(): TransactionFingerprint =
        TransactionFingerprint(date = _date, description = _description, _amount = _amount, _balance = _balance)

    fun changeCategory(newCategoryId: CategoryId) {
        requireNotNull(newCategoryId) { "Category id is required" }
        _categoryId = newCategoryId
        _categoryAssignmentSource = CategoryAssignmentSource.MANUAL
    }

    fun reassignCategory(newCategoryId: CategoryId) {
        requireNotNull(newCategoryId) { "Category id is required" }
        _categoryId = newCategoryId
    }

    fun applyRuleCategory(newCategoryId: CategoryId) {
        requireNotNull(newCategoryId) { "Category id is required" }
        if (isManuallyCategorised()) {
            throw IllegalStateException("Cannot overwrite a manual category with a rule")
        }
        _categoryId = newCategoryId
        _categoryAssignmentSource = CategoryAssignmentSource.RULE
    }

    fun withImportBatchId(importBatchId: ImportBatchId): Transaction {
        requireNotNull(importBatchId) { "Import batch id is required" }
        if (_importBatchId != null) {
            throw IllegalStateException("Transaction already belongs to an import batch")
        }
        return Transaction(
            _id, _accountId, importBatchId, _date, _amount, _description, _balance,
            _categoryId, _categoryAssignmentSource,
        )
    }

    companion object {
        @JvmStatic
        fun create(accountId: AccountId, row: ParsedTransactionRow, categoryId: CategoryId): Transaction =
            create(accountId, row, categoryId, null)

        @JvmStatic
        fun create(
            accountId: AccountId,
            row: ParsedTransactionRow,
            categoryId: CategoryId,
            importBatchId: ImportBatchId?,
        ): Transaction {
            requireNotNull(categoryId) { "Category id is required" }
            return Transaction(
                TransactionId.generate(),
                accountId,
                importBatchId,
                row.date,
                TransactionFingerprint.canonicalMoney(row.amount),
                row.description,
                TransactionFingerprint.canonicalMoney(row.balance),
                categoryId,
                CategoryAssignmentSource.RULE,
            )
        }

        @JvmStatic
        fun reconstitute(
            id: TransactionId,
            accountId: AccountId,
            importBatchId: ImportBatchId?,
            date: LocalDate,
            amount: BigDecimal,
            description: String,
            balance: BigDecimal,
            categoryId: CategoryId,
            categoryAssignmentSource: CategoryAssignmentSource,
        ): Transaction = Transaction(
            id, accountId, importBatchId, date, amount, description, balance,
            categoryId, categoryAssignmentSource,
        )
    }
}