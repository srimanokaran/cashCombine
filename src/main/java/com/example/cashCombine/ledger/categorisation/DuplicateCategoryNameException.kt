package com.example.cashCombine.ledger.categorisation

class DuplicateCategoryNameException(name: String) : RuntimeException("Category already exists: $name")