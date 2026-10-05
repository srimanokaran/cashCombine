package com.example.cashCombine.ledger.imports

class InvalidCsvRowException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)