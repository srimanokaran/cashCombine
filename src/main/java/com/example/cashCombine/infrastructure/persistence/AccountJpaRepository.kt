package com.example.cashCombine.infrastructure.persistence

import com.example.cashCombine.ledger.accounts.AccountType
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface AccountJpaRepository : JpaRepository<AccountJpaEntity, UUID> {

    fun findFirstByType(type: AccountType): AccountJpaEntity?
}