package com.example.cashCombine.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClassificationRuleJpaRepository extends JpaRepository<ClassificationRuleJpaEntity, UUID> {

	List<ClassificationRuleJpaEntity> findAllByOrderByCreatedOrderAsc();

	@Query("select coalesce(max(r.createdOrder), 0) from ClassificationRuleJpaEntity r")
	long findMaxCreatedOrder();

	void deleteByCategoryId(UUID categoryId);

}
