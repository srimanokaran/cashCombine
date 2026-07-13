package com.example.cashCombine.infrastructure.persistence;

import com.example.cashCombine.ledger.categorisation.Category;
import com.example.cashCombine.ledger.categorisation.CategoryId;
import com.example.cashCombine.ledger.categorisation.CategoryRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JpaCategoryRepository implements CategoryRepository {

	private final CategoryJpaRepository jpaRepository;

	public JpaCategoryRepository(CategoryJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Category save(Category category) {
		jpaRepository.save(new CategoryJpaEntity(category.id().value(), category.name()));
		return category;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Category> findById(CategoryId id) {
		return jpaRepository.findById(id.value()).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Category> findByName(String name) {
		return jpaRepository.findByNameIgnoreCase(name).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Category> findAll() {
		return jpaRepository.findAll().stream().map(this::toDomain).toList();
	}

	private Category toDomain(CategoryJpaEntity entity) {
		return Category.reconstitute(new CategoryId(entity.getId()), entity.getName());
	}

}
