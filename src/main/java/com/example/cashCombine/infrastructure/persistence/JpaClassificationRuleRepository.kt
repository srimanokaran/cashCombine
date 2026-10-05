package com.example.cashCombine.infrastructure.persistence

import com.example.cashCombine.ledger.categorisation.CategoryId
import com.example.cashCombine.ledger.categorisation.ClassificationRule
import com.example.cashCombine.ledger.categorisation.ClassificationRuleId
import com.example.cashCombine.ledger.categorisation.ClassificationRuleRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaClassificationRuleRepository(private val jpaRepository: ClassificationRuleJpaRepository) :
    ClassificationRuleRepository {

    override fun save(rule: ClassificationRule): ClassificationRule {
        val nextOrder = jpaRepository.findMaxCreatedOrder() + 1
        jpaRepository.save(ClassificationRuleJpaEntity(
            rule.id.value, rule.pattern, rule.categoryId.value, nextOrder))
        return rule
    }

    @Transactional(readOnly = true)
    override fun findById(id: ClassificationRuleId): ClassificationRule? {
        return jpaRepository.findById(id.value).map { toDomain(it) }.orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<ClassificationRule> {
        return jpaRepository.findAllByOrderByCreatedOrderAsc().map { toDomain(it) }
    }

    override fun deleteById(id: ClassificationRuleId) {
        jpaRepository.deleteById(id.value)
    }

    override fun deleteByCategoryId(categoryId: CategoryId) {
        jpaRepository.deleteByCategoryId(categoryId.value)
    }

    private fun toDomain(entity: ClassificationRuleJpaEntity): ClassificationRule {
        return ClassificationRule.reconstitute(
            ClassificationRuleId(entity.id!!),
            entity.pattern!!,
            CategoryId(entity.categoryId!!))
    }
}