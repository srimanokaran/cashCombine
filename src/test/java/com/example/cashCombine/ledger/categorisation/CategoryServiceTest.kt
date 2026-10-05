package com.example.cashCombine.ledger.categorisation

import com.example.cashCombine.ledger.accounts.AccountId
import com.example.cashCombine.ledger.imports.ParsedTransactionRow
import com.example.cashCombine.ledger.transactions.InMemoryTransactionRepository
import com.example.cashCombine.ledger.transactions.Transaction
import com.example.cashCombine.ledger.transactions.TransactionRepository
import java.math.BigDecimal
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CategoryServiceTest {

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var ruleRepository: ClassificationRuleRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryService: CategoryService
    private lateinit var uncategorised: Category

    @BeforeEach
    fun setUp() {
        categoryRepository = InMemoryCategoryRepository()
        ruleRepository = InMemoryClassificationRuleRepository()
        transactionRepository = InMemoryTransactionRepository()
        uncategorised = categoryRepository.save(Category.uncategorised())
        categoryService = CategoryService(categoryRepository, ruleRepository, transactionRepository)
    }

    @Test
    fun `creates and lists categories`() {
        val groceries = categoryService.createCategory("Groceries")
        val dining = categoryService.createCategory("Dining")

        assertThat(categoryService.listCategories()).containsExactlyInAnyOrder(uncategorised, groceries, dining)
    }

    @Test
    fun `rejects duplicate category name ignoring case`() {
        categoryService.createCategory("Groceries")

        assertThatThrownBy { categoryService.createCategory("groceries") }
            .isInstanceOf(DuplicateCategoryNameException::class.java)
    }

    @Test
    fun `rejects blank category name`() {
        assertThatThrownBy { categoryService.createCategory("  ") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("name")
    }

    @Test
    fun `deletes category and cascades rules and transactions`() {
        val groceries = categoryService.createCategory("Groceries")
        ruleRepository.save(ClassificationRule.create("WOOLWORTHS", groceries.id))
        val transaction = transactionRepository.save(
            Transaction.create(
                AccountId.generate(),
                ParsedTransactionRow(
                    LocalDate.of(2026, 7, 10),
                    BigDecimal("-12.50"),
                    "WOOLWORTHS",
                    BigDecimal("100.00")
                ),
                groceries.id
            )
        )

        categoryService.deleteCategory(groceries.id)

        assertThat(categoryRepository.findById(groceries.id)).isNull()
        assertThat(ruleRepository.findAll()).isEmpty()
        val updated = transactionRepository.findById(transaction.id())!!
        assertThat(updated.categoryId()).isEqualTo(uncategorised.id)
        assertThat(updated.categoryAssignmentSource()).isEqualTo(CategoryAssignmentSource.RULE)
    }

    @Test
    fun `rejects deleting Uncategorised`() {
        assertThatThrownBy { categoryService.deleteCategory(uncategorised.id) }
            .isInstanceOf(ProtectedCategoryException::class.java)
    }

    @Test
    fun `delete rejects unknown category`() {
        assertThatThrownBy { categoryService.deleteCategory(CategoryId.generate()) }
            .isInstanceOf(CategoryNotFoundException::class.java)
    }
}