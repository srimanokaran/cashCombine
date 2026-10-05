package com.example.cashCombine.ledger.transactions

import com.example.cashCombine.ledger.accounts.AccountId
import com.example.cashCombine.ledger.categorisation.CategoryId
import com.example.cashCombine.ledger.categorisation.CategoryNotFoundException
import com.example.cashCombine.ledger.categorisation.CategoryRepository
import com.example.cashCombine.ledger.categorisation.ClassificationRule
import com.example.cashCombine.ledger.categorisation.ClassificationRuleService
import com.example.cashCombine.ledger.categorisation.RulePattern
import org.springframework.transaction.annotation.Transactional

open class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val classificationRuleService: ClassificationRuleService,
) {

    @Transactional
    open fun changeCategory(transactionId: TransactionId, categoryId: CategoryId): Transaction {
        val transaction = transactionRepository.findById(transactionId)
            ?: throw TransactionNotFoundException(transactionId)
        if (categoryRepository.findById(categoryId) == null) {
            throw CategoryNotFoundException(categoryId)
        }

        transaction.changeCategory(categoryId)
        val saved = transactionRepository.save(transaction)

        val rule = classificationRuleService.upsertRule(
            RulePattern.fromDescription(saved.description()), categoryId)
        applyRuleToMatchingTransactions(rule, saved.id())

        return saved
    }

    private fun applyRuleToMatchingTransactions(rule: ClassificationRule, skipId: TransactionId) {
        for (other in transactionRepository.findAll()) {
            if (other.id() == skipId) continue
            if (other.isManuallyCategorised()) continue
            if (!rule.matches(other.description())) continue
            if (other.categoryId() == rule.categoryId) continue
            other.applyRuleCategory(rule.categoryId)
            transactionRepository.save(other)
        }
    }

    open fun getTransaction(transactionId: TransactionId): Transaction =
        transactionRepository.findById(transactionId) ?: throw TransactionNotFoundException(transactionId)

    open fun listByAccount(accountId: AccountId): List<Transaction> =
        transactionRepository.findByAccountId(accountId)
}