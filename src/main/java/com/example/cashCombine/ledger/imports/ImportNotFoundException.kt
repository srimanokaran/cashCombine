package com.example.cashCombine.ledger.imports

class ImportNotFoundException(id: ImportBatchId) : RuntimeException("Import not found: ${id.value}")