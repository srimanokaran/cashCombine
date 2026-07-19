package com.example.cashCombine.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImportBatchJpaRepository extends JpaRepository<ImportBatchJpaEntity, UUID> {

	List<ImportBatchJpaEntity> findByAccountIdOrderByImportedAtDesc(UUID accountId);

	@Modifying(clearAutomatically = true)
	@Query("delete from ImportBatchJpaEntity b where b.accountId = :accountId")
	void deleteByAccountId(@Param("accountId") UUID accountId);

}
