package com.example.cashCombine.ledger.transactions

class TransactionNotFoundException(id: TransactionId) : RuntimeException("Transaction not found: ${id.value}")