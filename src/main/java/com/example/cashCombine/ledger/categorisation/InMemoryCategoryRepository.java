package com.example.cashCombine.ledger.categorisation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryCategoryRepository implements CategoryRepository {

	private final Map<CategoryId, Category> categories = new HashMap<>();

	@Override
	public Category save(Category category) {
		categories.put(category.id(), category);
		return category;
	}

	@Override
	public Optional<Category> findById(CategoryId id) {
		return Optional.ofNullable(categories.get(id));
	}

	@Override
	public Optional<Category> findByName(String name) {
		return categories.values().stream()
				.filter(category -> category.name().equalsIgnoreCase(name))
				.findFirst();
	}

	@Override
	public List<Category> findAll() {
		return new ArrayList<>(categories.values());
	}

	@Override
	public void deleteById(CategoryId id) {
		categories.remove(id);
	}

}
