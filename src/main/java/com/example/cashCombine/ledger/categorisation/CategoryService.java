package com.example.cashCombine.ledger.categorisation;

import java.util.List;

public class CategoryService {

	private final CategoryRepository categoryRepository;

	public CategoryService(CategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	public Category createCategory(String name) {
		Category category = Category.create(name);
		if (categoryRepository.findByName(category.name()).isPresent()) {
			throw new DuplicateCategoryNameException(category.name());
		}
		return categoryRepository.save(category);
	}

	public List<Category> listCategories() {
		return categoryRepository.findAll();
	}

	public Category getCategory(CategoryId id) {
		return categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
	}

}
