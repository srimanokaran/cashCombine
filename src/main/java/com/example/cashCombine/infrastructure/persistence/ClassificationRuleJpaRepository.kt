package com.example.cashCombine.infrastructure.persistence

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ClassificationRuleJpaRepository : JpaRepository<ClassificationRuleJpaEntity, UUID> {

    fun findAllByOrderByCreatedOrderAsc(): List<ClassificationRuleJpaEntity>

    @Query("select coalesce(max(r.createdOrder), 0) from ClassificationRuleJpaEntity r")
    fun findMaxCreatedOrder(): Long

    fun deleteByCategoryId(categoryId: UUID)
}