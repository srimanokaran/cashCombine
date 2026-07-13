package com.example.cashCombine.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, UUID> {

	Optional<CategoryJpaEntity> findByNameIgnoreCase(String name);

}
