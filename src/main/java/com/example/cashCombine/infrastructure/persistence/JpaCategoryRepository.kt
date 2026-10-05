package com.example.cashCombine.infrastructure.persistence

import com.example.cashCombine.ledger.categorisation.Category
import com.example.cashCombine.ledger.categorisation.CategoryId
import com.example.cashCombine.ledger.categorisation.CategoryRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaCategoryRepository(private val jpaRepository: CategoryJpaRepository) : CategoryRepository {

    override fun save(category: Category): Category {
        jpaRepository.save(CategoryJpaEntity(category.id.value, category.name))
        return category
    }

    @Transactional(readOnly = true)
    override fun findById(id: CategoryId): Category? {
        return jpaRepository.findById(id.value).map { toDomain(it) }.orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByName(name: String): Category? {
        return jpaRepository.findByNameIgnoreCase(name)?.let { toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<Category> {
        return jpaRepository.findAll().map { toDomain(it) }
    }

    override fun deleteById(id: CategoryId) {
        jpaRepository.deleteById(id.value)
    }

    private fun toDomain(entity: CategoryJpaEntity): Category {
        return Category.reconstitute(CategoryId(entity.id!!), entity.name!!)
    }
}