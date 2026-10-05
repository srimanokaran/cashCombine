package com.example.cashCombine.ledger.accounts

class AccountNotFoundException(id: AccountId) : RuntimeException("Account not found: ${id.value}")