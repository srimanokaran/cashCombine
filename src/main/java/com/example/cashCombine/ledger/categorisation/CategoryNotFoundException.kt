package com.example.cashCombine.ledger.categorisation

class CategoryNotFoundException(id: CategoryId) : RuntimeException("Category not found: ${id.value}")