package com.example.cashCombine.ledger.categorisation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoryServiceTest {

	private CategoryService categoryService;

	@BeforeEach
	void setUp() {
		categoryService = new CategoryService(new InMemoryCategoryRepository());
	}

	@Test
	void createsAndListsCategories() {
		Category groceries = categoryService.createCategory("Groceries");
		Category dining = categoryService.createCategory("Dining");

		assertThat(categoryService.listCategories()).containsExactlyInAnyOrder(groceries, dining);
	}

	@Test
	void rejectsDuplicateCategoryNameIgnoringCase() {
		categoryService.createCategory("Groceries");

		assertThatThrownBy(() -> categoryService.createCategory("groceries"))
				.isInstanceOf(DuplicateCategoryNameException.class);
	}

	@Test
	void rejectsBlankCategoryName() {
		assertThatThrownBy(() -> categoryService.createCategory("  "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("name");
	}

}
