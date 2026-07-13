package com.example.cashCombine.ledger.categorisation;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

	Category save(Category category);

	Optional<Category> findById(CategoryId id);

	Optional<Category> findByName(String name);

	List<Category> findAll();

	void deleteById(CategoryId id);

}
