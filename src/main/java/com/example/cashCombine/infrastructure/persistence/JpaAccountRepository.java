package com.example.cashCombine.infrastructure.persistence;

import com.example.cashCombine.ledger.accounts.Account;
import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.accounts.AccountRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JpaAccountRepository implements AccountRepository {

	private final AccountJpaRepository jpaRepository;

	public JpaAccountRepository(AccountJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Account save(Account account) {
		AccountJpaEntity entity = new AccountJpaEntity(
				account.id().value(), account.name(), account.type(), account.hasImports());
		jpaRepository.save(entity);
		return account;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Account> findById(AccountId id) {
		return jpaRepository.findById(id.value()).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Account> findAll() {
		return jpaRepository.findAll().stream().map(this::toDomain).toList();
	}

	@Override
	public void deleteById(AccountId id) {
		jpaRepository.deleteById(id.value());
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsById(AccountId id) {
		return jpaRepository.existsById(id.value());
	}

	private Account toDomain(AccountJpaEntity entity) {
		return Account.reconstitute(
				new AccountId(entity.getId()), entity.getName(), entity.getType(), entity.isHasImports());
	}

}
