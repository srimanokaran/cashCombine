package com.example.cashCombine.infrastructure.persistence

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TransactionJpaRepository : JpaRepository<TransactionJpaEntity, UUID> {

    fun findByAccountId(accountId: UUID): List<TransactionJpaEntity>

    fun findByCategoryId(categoryId: UUID): List<TransactionJpaEntity>

    fun existsByAccountId(accountId: UUID): Boolean

    fun existsByAccountIdAndDateAndAmountAndDescriptionAndBalance(
        accountId: UUID, date: LocalDate, amount: BigDecimal, description: String, balance: BigDecimal): Boolean

    @Modifying(clearAutomatically = true)
    @Query("delete from TransactionJpaEntity t where t.accountId = :accountId")
    fun deleteByAccountId(@Param("accountId") accountId: UUID)

    @Modifying(clearAutomatically = true)
    @Query("delete from TransactionJpaEntity t where t.importBatchId = :importBatchId")
    fun deleteByImportBatchId(@Param("importBatchId") importBatchId: UUID)
}