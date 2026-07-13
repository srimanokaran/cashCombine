package com.example.cashCombine.infrastructure.persistence;

import com.example.cashCombine.ledger.categorisation.CategoryId;
import com.example.cashCombine.ledger.categorisation.ClassificationRule;
import com.example.cashCombine.ledger.categorisation.ClassificationRuleId;
import com.example.cashCombine.ledger.categorisation.ClassificationRuleRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JpaClassificationRuleRepository implements ClassificationRuleRepository {

	private final ClassificationRuleJpaRepository jpaRepository;

	public JpaClassificationRuleRepository(ClassificationRuleJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public ClassificationRule save(ClassificationRule rule) {
		long nextOrder = jpaRepository.findMaxCreatedOrder() + 1;
		jpaRepository.save(new ClassificationRuleJpaEntity(
				rule.id().value(), rule.pattern(), rule.categoryId().value(), nextOrder));
		return rule;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ClassificationRule> findById(ClassificationRuleId id) {
		return jpaRepository.findById(id.value()).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ClassificationRule> findAll() {
		return jpaRepository.findAllByOrderByCreatedOrderAsc().stream().map(this::toDomain).toList();
	}

	@Override
	public void deleteById(ClassificationRuleId id) {
		jpaRepository.deleteById(id.value());
	}

	private ClassificationRule toDomain(ClassificationRuleJpaEntity entity) {
		return ClassificationRule.reconstitute(
				new ClassificationRuleId(entity.getId()),
				entity.getPattern(),
				new CategoryId(entity.getCategoryId()));
	}

}
