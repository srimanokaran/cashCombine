package com.example.cashCombine.infrastructure.persistence;

import com.example.cashCombine.ledger.accounts.AccountType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, UUID> {

	Optional<AccountJpaEntity> findFirstByType(AccountType type);

}
