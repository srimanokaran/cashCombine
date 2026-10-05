package com.example.cashCombine.ledger.transactions

import com.example.cashCombine.ledger.accounts.AccountId
import com.example.cashCombine.ledger.categorisation.Category
import com.example.cashCombine.ledger.categorisation.CategoryAssignmentSource
import com.example.cashCombine.ledger.categorisation.CategoryId
import com.example.cashCombine.ledger.categorisation.CategoryNotFoundException
import com.example.cashCombine.ledger.categorisation.CategoryRepository
import com.example.cashCombine.ledger.categorisation.ClassificationRule
import com.example.cashCombine.ledger.categorisation.ClassificationRuleService
import com.example.cashCombine.ledger.categorisation.InMemoryCategoryRepository
import com.example.cashCombine.ledger.categorisation.InMemoryClassificationRuleRepository
import com.example.cashCombine.ledger.imports.ParsedTransactionRow
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class TransactionServiceTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var ruleRepository: InMemoryClassificationRuleRepository
    private lateinit var transactionService: TransactionService
    private lateinit var uncategorised: Category
    private lateinit var groceries: Category
    private lateinit var dining: Category

    @BeforeEach
    fun setUp() {
        transactionRepository = InMemoryTransactionRepository()
        categoryRepository = InMemoryCategoryRepository()
        ruleRepository = InMemoryClassificationRuleRepository()
        uncategorised = categoryRepository.save(Category.uncategorised())
        groceries = categoryRepository.save(Category.create("Groceries"))
        dining = categoryRepository.save(Category.create("Dining"))
        val ruleService =
            ClassificationRuleService(ruleRepository, categoryRepository)
        transactionService = TransactionService(transactionRepository, categoryRepository, ruleService)
    }

    @Test
    fun `change category overrides rule assignment and creates rule`() {
        val transaction = transactionRepository.save(sampleTransaction(uncategorised.id))
        assertThat(transaction.categoryAssignmentSource()).isEqualTo(CategoryAssignmentSource.RULE)

        val updated = transactionService.changeCategory(transaction.id(), dining.id)

        assertThat(updated.categoryId()).isEqualTo(dining.id)
        assertThat(updated.isManuallyCategorised()).isTrue()
        assertThat(transactionService.getTransaction(transaction.id()).categoryId()).isEqualTo(dining.id)

        val rules = ruleRepository.findAll()
        assertThat(rules).hasSize(1)
        assertThat(rules[0].pattern).isEqualTo(CAFE_DESCRIPTION)
        assertThat(rules[0].categoryId).isEqualTo(dining.id)
    }

    @Test
    fun `change category applies rule to matching non-manual transactions`() {
        val primary = transactionRepository.save(sampleTransaction(uncategorised.id))
        val sibling = transactionRepository.save(sampleTransaction(
            AccountId.generate(), uncategorised.id, CAFE_DESCRIPTION, LocalDate.of(2026, 7, 11)))
        val otherManual = transactionRepository.save(sampleTransaction(
            AccountId.generate(), uncategorised.id, CAFE_DESCRIPTION, LocalDate.of(2026, 7, 12)))
        otherManual.changeCategory(groceries.id)
        transactionRepository.save(otherManual)
        val unrelated = transactionRepository.save(sampleTransaction(
            AccountId.generate(), uncategorised.id, "WOOLWORTHS 1234", LocalDate.of(2026, 7, 13)))

        transactionService.changeCategory(primary.id(), dining.id)

        assertThat(transactionRepository.findById(sibling.id())!!.categoryId())
            .isEqualTo(dining.id)
        assertThat(transactionRepository.findById(sibling.id())!!.categoryAssignmentSource())
            .isEqualTo(CategoryAssignmentSource.RULE)
        assertThat(transactionRepository.findById(otherManual.id())!!.categoryId())
            .isEqualTo(groceries.id)
        assertThat(transactionRepository.findById(unrelated.id())!!.categoryId())
            .isEqualTo(uncategorised.id)
    }

    @Test
    fun `change category strips card and value date noise from rule pattern`() {
        val first =
            "COMFY.ORG SAN FRANCISCO CA USA Card xx3430 USD 20.00 Value Date: 18/08/2026"
        val siblingDesc =
            "COMFY.ORG SAN FRANCISCO CA USA Card xx3430 USD 5.00 Value Date: 11/09/2026"
        val primary = transactionRepository.save(sampleTransaction(
            AccountId.generate(), uncategorised.id, first, LocalDate.of(2026, 8, 18)))
        val sibling = transactionRepository.save(sampleTransaction(
            AccountId.generate(), uncategorised.id, siblingDesc, LocalDate.of(2026, 9, 11)))

        transactionService.changeCategory(primary.id(), dining.id)

        val createdRule = ruleRepository.findAll().single()
        assertThat(createdRule.pattern).isEqualTo("COMFY.ORG SAN FRANCISCO CA USA")
        assertThat(createdRule.categoryId).isEqualTo(dining.id)
        assertThat(transactionRepository.findById(sibling.id())!!.categoryId())
            .isEqualTo(dining.id)
    }

    @Test
    fun `change category retargets existing same pattern rule`() {
        ruleRepository.save(ClassificationRule.create(CAFE_DESCRIPTION, groceries.id))
        val transaction = transactionRepository.save(sampleTransaction(uncategorised.id))

        transactionService.changeCategory(transaction.id(), dining.id)

        val rules = ruleRepository.findAll()
        assertThat(rules).hasSize(1)
        assertThat(rules[0].pattern).isEqualTo(CAFE_DESCRIPTION)
        assertThat(rules[0].categoryId).isEqualTo(dining.id)
    }

    @Test
    fun `change category rejects unknown transaction`() {
        assertThatThrownBy { transactionService.changeCategory(TransactionId.generate(), groceries.id) }
            .isInstanceOf(TransactionNotFoundException::class.java)
    }

    @Test
    fun `change category rejects unknown category`() {
        val transaction = transactionRepository.save(sampleTransaction(uncategorised.id))

        assertThatThrownBy { transactionService.changeCategory(transaction.id(), CategoryId.generate()) }
            .isInstanceOf(CategoryNotFoundException::class.java)
    }

    @Test
    fun `bank facts remain unchanged after category override`() {
        val transaction = transactionRepository.save(sampleTransaction(uncategorised.id))
        val originalDate = transaction.date()
        val originalAmount = transaction.amount()
        val originalDescription = transaction.description()

        transactionService.changeCategory(transaction.id(), groceries.id)
        val updated = transactionService.getTransaction(transaction.id())

        assertThat(updated.date()).isEqualTo(originalDate)
        assertThat(updated.amount()).isEqualByComparingTo(originalAmount)
        assertThat(updated.description()).isEqualTo(originalDescription)
    }

    @Test
    fun `lists transactions for account`() {
        val accountId = AccountId.generate()
        val first = transactionRepository.save(sampleTransaction(accountId, uncategorised.id))
        val second = transactionRepository.save(
            sampleTransaction(accountId, groceries.id, "WOOLWORTHS 1234", LocalDate.of(2026, 7, 9)))
        transactionRepository.save(sampleTransaction(AccountId.generate(), dining.id))

        assertThat(transactionService.listByAccount(accountId)).containsExactlyInAnyOrder(first, second)
    }

    private fun sampleTransaction(categoryId: CategoryId): Transaction =
        sampleTransaction(AccountId.generate(), categoryId)

    private fun sampleTransaction(accountId: AccountId, categoryId: CategoryId): Transaction =
        sampleTransaction(accountId, categoryId, CAFE_DESCRIPTION, LocalDate.of(2026, 7, 10))

    private fun sampleTransaction(
        accountId: AccountId, categoryId: CategoryId, description: String, date: LocalDate,
    ): Transaction = Transaction.create(
        accountId,
        ParsedTransactionRow(date, BigDecimal("-12.50"), description, BigDecimal("2467.50")),
        categoryId)

    companion object {
        private const val CAFE_DESCRIPTION = "CAFE EXAMPLE BLEND FAKETOWN AUS"
    }
}