package com.example.cashCombine.api.accounts;

import com.example.cashCombine.ledger.imports.ImportResult;

public record ImportResultResponse(int accepted, int duplicate, int rejected) {

	public static ImportResultResponse from(ImportResult result) {
		return new ImportResultResponse(result.accepted(), result.duplicate(), result.rejected());
	}

}
