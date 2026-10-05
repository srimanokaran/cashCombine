package com.example.cashCombine.ledger.categorisation

class InMemoryCategoryRepository : CategoryRepository {

    private val categories = HashMap<CategoryId, Category>()

    override fun save(category: Category): Category {
        categories[category.id] = category
        return category
    }

    override fun findById(id: CategoryId): Category? = categories[id]

    override fun findByName(name: String): Category? =
        categories.values.filter { it.name.equals(name, ignoreCase = true) }.firstOrNull()

    override fun findAll(): List<Category> = ArrayList(categories.values)

    override fun deleteById(id: CategoryId) {
        categories.remove(id)
    }
}