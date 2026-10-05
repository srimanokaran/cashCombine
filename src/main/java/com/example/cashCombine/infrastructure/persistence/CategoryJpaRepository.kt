package com.example.cashCombine.infrastructure.persistence

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryJpaRepository : JpaRepository<CategoryJpaEntity, UUID> {

    fun findByNameIgnoreCase(name: String): CategoryJpaEntity?
}