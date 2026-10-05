package com.example.cashCombine.infrastructure.persistence

import com.example.cashCombine.ledger.accounts.Account
import com.example.cashCombine.ledger.accounts.AccountId
import com.example.cashCombine.ledger.accounts.AccountRepository
import com.example.cashCombine.ledger.accounts.AccountType
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaAccountRepository(private val jpaRepository: AccountJpaRepository) : AccountRepository {

    override fun save(account: Account): Account {
        val entity = AccountJpaEntity(
            account.id().value, account.name(), account.type(), account.hasImports())
        jpaRepository.save(entity)
        return account
    }

    @Transactional(readOnly = true)
    override fun findById(id: AccountId): Account? {
        return jpaRepository.findById(id.value).map { toDomain(it) }.orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByType(type: AccountType): Account? {
        return jpaRepository.findFirstByType(type)?.let { toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<Account> {
        return jpaRepository.findAll().map { toDomain(it) }
    }

    override fun deleteById(id: AccountId) {
        jpaRepository.deleteById(id.value)
    }

    @Transactional(readOnly = true)
    override fun existsById(id: AccountId): Boolean {
        return jpaRepository.existsById(id.value)
    }

    private fun toDomain(entity: AccountJpaEntity): Account {
        return Account.reconstitute(
            AccountId(entity.id!!), entity.name!!, entity.type!!, entity.hasImports)
    }
}