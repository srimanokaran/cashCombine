package com.example.cashCombine.ledger.categorisation

class ProtectedCategoryException(name: String) : RuntimeException("Cannot delete protected category: $name")