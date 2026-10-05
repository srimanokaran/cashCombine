package com.example.cashCombine.ledger.categorisation

import com.example.cashCombine.ledger.accounts.AccountId
import com.example.cashCombine.ledger.imports.ParsedTransactionRow
import com.example.cashCombine.ledger.transactions.InMemoryTransactionRepository
import com.example.cashCombine.ledger.transactions.Transaction
import java.math.BigDecimal
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CategoryReanalysisServiceTest {

    private lateinit var categoryRepository: InMemoryCategoryRepository
    private lateinit var ruleRepository: InMemoryClassificationRuleRepository
    private lateinit var transactionRepository: InMemoryTransactionRepository
    private lateinit var reanalysisService: CategoryReanalysisService
    private lateinit var uncategorised: Category
    private lateinit var fuel: Category
    private lateinit var rent: Category

    @BeforeEach
    fun setUp() {
        categoryRepository = InMemoryCategoryRepository()
        ruleRepository = InMemoryClassificationRuleRepository()
        transactionRepository = InMemoryTransactionRepository()
        uncategorised = categoryRepository.save(Category.uncategorised())
        fuel = categoryRepository.save(Category.create("Fuel"))
        rent = categoryRepository.save(Category.create("Rent"))
        val classifier = TransactionClassifier(ruleRepository, uncategorised.id)
        reanalysisService = CategoryReanalysisService(transactionRepository, classifier)
    }

    @Test
    fun `reassigns rule-based transactions and skips manual overrides`() {
        ruleRepository.save(ClassificationRule.create("BP ", fuel.id))
        ruleRepository.save(ClassificationRule.create("Transfer To Landlord", rent.id))

        val bpay = transactionRepository.save(tx("Qantas Credit Cards CommBank app BPAY Bill", fuel.id))
        val bpPetrol = transactionRepository.save(tx("BP EXPRESS HIGHWAY", uncategorised.id))
        val rentTransfer = transactionRepository.save(tx("Transfer To Landlord CommBank App Rent", uncategorised.id))
        val manual = transactionRepository.save(tx("Gift for friend", uncategorised.id))
        manual.changeCategory(rent.id)
        transactionRepository.save(manual)

        val result = reanalysisService.reanalyse()

        assertThat(result.examined).isEqualTo(4)
        assertThat(result.updated).isEqualTo(3)
        assertThat(result.skippedManual).isEqualTo(1)

        assertThat(transactionRepository.findById(bpay.id())!!.categoryId())
            .isEqualTo(uncategorised.id)
        assertThat(transactionRepository.findById(bpPetrol.id())!!.categoryId())
            .isEqualTo(fuel.id)
        assertThat(transactionRepository.findById(rentTransfer.id())!!.categoryId()).isEqualTo(rent.id)
        assertThat(transactionRepository.findById(manual.id())!!.categoryId()).isEqualTo(rent.id)
        assertThat(transactionRepository.findById(manual.id())!!.isManuallyCategorised()).isTrue
    }

    private fun tx(description: String, categoryId: CategoryId): Transaction =
        Transaction.create(
            AccountId.generate(),
            ParsedTransactionRow(
                LocalDate.of(2026, 7, 10),
                BigDecimal("-50.00"),
                description,
                BigDecimal("100.00")
            ),
            categoryId
        )
}