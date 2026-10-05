package com.example.cashCombine.ledger.accounts

class InMemoryAccountRepository : AccountRepository {

    private val accounts = mutableMapOf<AccountId, Account>()

    override fun save(account: Account): Account {
        accounts[account.id()] = account
        return account
    }

    override fun findById(id: AccountId): Account? = accounts[id]

    override fun findByType(type: AccountType): Account? = accounts.values.firstOrNull { it.type() == type }

    override fun findAll(): List<Account> = accounts.values.toList()

    override fun deleteById(id: AccountId) {
        accounts.remove(id)
    }

    override fun existsById(id: AccountId): Boolean = accounts.containsKey(id)
}