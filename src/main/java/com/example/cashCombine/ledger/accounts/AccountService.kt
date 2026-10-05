package com.example.cashCombine.ledger.accounts

import com.example.cashCombine.ledger.imports.ImportBatchRepository
import com.example.cashCombine.ledger.transactions.TransactionRepository
import org.springframework.transaction.annotation.Transactional

@Transactional
open class AccountService(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val importBatchRepository: ImportBatchRepository,
) {

    open fun createAccount(name: String, type: AccountType): Account {
        val account = Account.create(name, type)
        return accountRepository.save(account)
    }

    @Synchronized
    open fun ensureFixedAccounts(): List<Account> {
        for (type in AccountType.entries) {
            if (accountRepository.findByType(type) == null) {
                accountRepository.save(Account.create(type.displayName, type))
            }
        }
        return listFixedAccounts()
    }

    open fun listFixedAccounts(): List<Account> =
        AccountType.entries
            .mapNotNull { accountRepository.findByType(it) }
            .sortedBy { it.type().ordinal }

    open fun listAccounts(): List<Account> = accountRepository.findAll()

    open fun deleteAccount(id: AccountId) {
        if (!accountRepository.existsById(id)) {
            throw AccountNotFoundException(id)
        }
        transactionRepository.deleteByAccountId(id)
        importBatchRepository.deleteByAccountId(id)
        accountRepository.deleteById(id)
    }

    open fun getAccount(id: AccountId): Account =
        accountRepository.findById(id) ?: throw AccountNotFoundException(id)
}