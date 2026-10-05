package com.example.cashCombine.ledger.imports

import com.example.cashCombine.ledger.accounts.AccountId
import com.example.cashCombine.ledger.categorisation.Category
import com.example.cashCombine.ledger.categorisation.CategoryAssignmentSource
import com.example.cashCombine.ledger.transactions.InMemoryTransactionRepository
import com.example.cashCombine.ledger.transactions.Transaction
import com.example.cashCombine.ledger.transactions.TransactionId
import java.math.BigDecimal
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OrphanImportBackfillTest {

    @Test
    fun `groups orphans into one legacy batch per account`() {
        val transactions = InMemoryTransactionRepository()
        val batches = InMemoryImportBatchRepository()
        val firstAccount = AccountId.generate()
        val secondAccount = AccountId.generate()
        val uncategorised = Category.uncategorised()

        transactions.save(orphan(firstAccount, uncategorised, "A", "10.00"))
        transactions.save(orphan(firstAccount, uncategorised, "B", "20.00"))
        transactions.save(orphan(secondAccount, uncategorised, "C", "30.00"))

        val accounts = OrphanImportBackfill.run(transactions, batches)

        assertThat(accounts).isEqualTo(2)
        assertThat(batches.findByAccountId(firstAccount)).hasSize(1)
        assertThat(batches.findByAccountId(secondAccount)).hasSize(1)

        val firstBatch = batches.findByAccountId(firstAccount)[0]
        assertThat(firstBatch.filename).isEqualTo(OrphanImportBackfill.LEGACY_FILENAME)
        assertThat(firstBatch.accepted).isEqualTo(2)
        assertThat(transactions.findByAccountId(firstAccount))
            .allMatch { tx -> firstBatch.id == tx.importBatchId() }

        assertThat(OrphanImportBackfill.run(transactions, batches)).isZero
    }

    companion object {
        private fun orphan(
            accountId: AccountId, category: Category, description: String, balance: String
        ): Transaction =
            Transaction.reconstitute(
                TransactionId.generate(),
                accountId,
                null,
                LocalDate.of(2026, 7, 10),
                BigDecimal("-5.00"),
                description,
                BigDecimal(balance),
                category.id,
                CategoryAssignmentSource.RULE
            )
    }
}