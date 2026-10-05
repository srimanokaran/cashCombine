package com.example.cashCombine.ledger.categorisation

import com.example.cashCombine.ledger.transactions.Transaction
import com.example.cashCombine.ledger.transactions.TransactionRepository
import org.springframework.transaction.annotation.Transactional

open class CategoryReanalysisService(
    private val transactionRepository: TransactionRepository,
    private val classifier: TransactionClassifier
) {

    @Transactional
    open fun reanalyse(): CategoryReanalysisResult {
        var examined = 0
        var updated = 0
        var skippedManual = 0

        for (transaction in transactionRepository.findAll()) {
            examined++
            if (transaction.isManuallyCategorised()) {
                skippedManual++
                continue
            }

            val classified = classifier.classify(transaction.description())
            if (classified == transaction.categoryId()) {
                continue
            }

            transaction.applyRuleCategory(classified)
            transactionRepository.save(transaction)
            updated++
        }

        return CategoryReanalysisResult(examined, updated, skippedManual)
    }
}