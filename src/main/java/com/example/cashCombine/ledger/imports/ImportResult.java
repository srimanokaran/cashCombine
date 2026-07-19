package com.example.cashCombine.ledger.imports;

public record ImportResult(ImportBatchId id, int accepted, int duplicate, int rejected) {
}
