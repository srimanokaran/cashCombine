package com.example.cashCombine.ledger.categorisation

import com.example.cashCombine.ledger.transactions.Transaction
import com.example.cashCombine.ledger.transactions.TransactionRepository
import org.springframework.transaction.annotation.Transactional

@Transactional
open class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val ruleRepository: ClassificationRuleRepository,
    private val transactionRepository: TransactionRepository
) {

    open fun createCategory(name: String): Category {
        val category = Category.create(name)
        if (categoryRepository.findByName(category.name) != null) {
            throw DuplicateCategoryNameException(category.name)
        }
        return categoryRepository.save(category)
    }

    open fun listCategories(): List<Category> = categoryRepository.findAll()

    open fun getCategory(id: CategoryId): Category =
        categoryRepository.findById(id) ?: throw CategoryNotFoundException(id)

    open fun deleteCategory(id: CategoryId) {
        val category = categoryRepository.findById(id) ?: throw CategoryNotFoundException(id)
        if (category.isUncategorised) {
            throw ProtectedCategoryException(category.name)
        }

        val uncategorised = categoryRepository
            .findByName(Category.UNCATEGORISED_NAME)
            ?: throw IllegalStateException("Uncategorised category is required")

        ruleRepository.deleteByCategoryId(id)
        for (transaction in transactionRepository.findByCategoryId(id)) {
            transaction.reassignCategory(uncategorised.id)
            transactionRepository.save(transaction)
        }
        categoryRepository.deleteById(id)
    }
}