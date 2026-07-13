package com.example.cashCombine.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionJpaRepository extends JpaRepository<TransactionJpaEntity, UUID> {

	List<TransactionJpaEntity> findByAccountId(UUID accountId);

	boolean existsByAccountIdAndDateAndAmountAndDescriptionAndBalance(
			UUID accountId, LocalDate date, BigDecimal amount, String description, BigDecimal balance);

	@Modifying(clearAutomatically = true)
	@Query("delete from TransactionJpaEntity t where t.accountId = :accountId")
	void deleteByAccountId(@Param("accountId") UUID accountId);

}
