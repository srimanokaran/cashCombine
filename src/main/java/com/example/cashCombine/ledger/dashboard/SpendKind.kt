package com.example.cashCombine.ledger.dashboard

import com.example.cashCombine.ledger.categorisation.Category
import java.math.BigDecimal

enum class SpendKind {
    EXCLUDED,
    INCOME,
    EXPENSE
}

/**
 * Single definition of how a transaction contributes to spend, shared by every view:
 * funds between accounts never count, positive credits in income-like categories are income,
 * and everything else nets against its category (so refunds reduce spend).
 */
object SpendClassifier {

    @JvmStatic
    fun classify(category: Category, amount: BigDecimal): SpendKind =
        when {
            category.isExcludedFromExpenses -> SpendKind.EXCLUDED
            category.isIncomeCreditCategory && amount.signum() > 0 -> SpendKind.INCOME
            category.isIncome -> SpendKind.EXCLUDED
            else -> SpendKind.EXPENSE
        }
}
