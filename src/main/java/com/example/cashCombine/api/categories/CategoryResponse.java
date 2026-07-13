package com.example.cashCombine.api.categories;

import com.example.cashCombine.ledger.categorisation.Category;
import java.util.UUID;

public record CategoryResponse(UUID id, String name) {

	public static CategoryResponse from(Category category) {
		return new CategoryResponse(category.id().value(), category.name());
	}

}
