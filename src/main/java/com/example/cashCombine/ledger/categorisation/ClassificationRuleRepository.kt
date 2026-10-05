package com.example.cashCombine.ledger.categorisation

interface ClassificationRuleRepository {

    fun save(rule: ClassificationRule): ClassificationRule

    fun findById(id: ClassificationRuleId): ClassificationRule?

    fun findAll(): List<ClassificationRule>

    fun deleteById(id: ClassificationRuleId)

    fun deleteByCategoryId(categoryId: CategoryId)
}