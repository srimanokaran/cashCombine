package com.example.cashCombine.ledger.categorisation;

import com.example.cashCombine.ledger.transactions.Transaction;
import com.example.cashCombine.ledger.transactions.TransactionRepository;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class CategoryService {

	private final CategoryRepository categoryRepository;
	private final ClassificationRuleRepository ruleRepository;
	private final TransactionRepository transactionRepository;

	public CategoryService(
			CategoryRepository categoryRepository,
			ClassificationRuleRepository ruleRepository,
			TransactionRepository transactionRepository) {
		this.categoryRepository = categoryRepository;
		this.ruleRepository = ruleRepository;
		this.transactionRepository = transactionRepository;
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

	public void deleteCategory(CategoryId id) {
		Category category = categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
		if (category.isUncategorised()) {
			throw new ProtectedCategoryException(category.name());
		}

		Category uncategorised = categoryRepository
				.findByName(Category.UNCATEGORISED_NAME)
				.orElseThrow(() -> new IllegalStateException("Uncategorised category is required"));

		ruleRepository.deleteByCategoryId(id);
		for (Transaction transaction : transactionRepository.findByCategoryId(id)) {
			transaction.reassignCategory(uncategorised.id());
			transactionRepository.save(transaction);
		}
		categoryRepository.deleteById(id);
	}

}
