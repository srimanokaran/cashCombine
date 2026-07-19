package com.example.cashCombine.api.dashboard;

import com.example.cashCombine.ledger.categorisation.CategoryReanalysisResult;

public record CategoryReanalysisResponse(int examined, int updated, int skippedManual) {

	public static CategoryReanalysisResponse from(CategoryReanalysisResult result) {
		return new CategoryReanalysisResponse(result.examined(), result.updated(), result.skippedManual());
	}

}
