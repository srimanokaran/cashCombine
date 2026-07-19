package com.example.cashCombine.api.accounts;

import com.example.cashCombine.ledger.imports.ImportResult;
import java.util.UUID;

public record ImportResultResponse(UUID id, int accepted, int duplicate, int rejected) {

	public static ImportResultResponse from(ImportResult result) {
		return new ImportResultResponse(
				result.id().value(), result.accepted(), result.duplicate(), result.rejected());
	}

}
