package com.example.cashCombine.infrastructure.persistence

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ImportBatchJpaRepository : JpaRepository<ImportBatchJpaEntity, UUID> {

    fun findByAccountIdOrderByImportedAtDesc(accountId: UUID): List<ImportBatchJpaEntity>

    @Modifying(clearAutomatically = true)
    @Query("delete from ImportBatchJpaEntity b where b.accountId = :accountId")
    fun deleteByAccountId(@Param("accountId") accountId: UUID)
}