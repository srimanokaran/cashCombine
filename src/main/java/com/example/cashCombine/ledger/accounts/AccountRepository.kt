package com.example.cashCombine.ledger.accounts

interface AccountRepository {

    fun save(account: Account): Account

    fun findById(id: AccountId): Account?

    fun findByType(type: AccountType): Account?

    fun findAll(): List<Account>

    fun deleteById(id: AccountId)

    fun existsById(id: AccountId): Boolean
}