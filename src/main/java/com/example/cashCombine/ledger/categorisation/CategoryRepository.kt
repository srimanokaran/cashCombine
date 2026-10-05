package com.example.cashCombine.ledger.categorisation

interface CategoryRepository {

    fun save(category: Category): Category

    fun findById(id: CategoryId): Category?

    fun findByName(name: String): Category?

    fun findAll(): List<Category>

    fun deleteById(id: CategoryId)
}