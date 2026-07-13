package com.example.cashCombine.infrastructure.persistence;

import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.categorisation.CategoryId;
import com.example.cashCombine.ledger.transactions.Transaction;
import com.example.cashCombine.ledger.transactions.TransactionFingerprint;
import com.example.cashCombine.ledger.transactions.TransactionId;
import com.example.cashCombine.ledger.transactions.TransactionRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JpaTransactionRepository implements TransactionRepository {

	private final TransactionJpaRepository jpaRepository;

	public JpaTransactionRepository(TransactionJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Transaction save(Transaction transaction) {
		TransactionJpaEntity entity = new TransactionJpaEntity(
				transaction.id().value(),
				transaction.accountId().value(),
				transaction.date(),
				transaction.amount(),
				transaction.description(),
				transaction.balance(),
				transaction.categoryId().value(),
				transaction.categoryAssignmentSource());
		jpaRepository.save(entity);
		return transaction;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Transaction> findById(TransactionId id) {
		return jpaRepository.findById(id.value()).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Transaction> findByAccountId(AccountId accountId) {
		return jpaRepository.findByAccountId(accountId.value()).stream().map(this::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByAccountAndFingerprint(AccountId accountId, TransactionFingerprint fingerprint) {
		return jpaRepository.existsByAccountIdAndDateAndAmountAndDescriptionAndBalance(
				accountId.value(),
				fingerprint.date(),
				fingerprint.amount(),
				fingerprint.description(),
				fingerprint.balance());
	}

	@Override
	public void deleteByAccountId(AccountId accountId) {
		jpaRepository.deleteByAccountId(accountId.value());
	}

	private Transaction toDomain(TransactionJpaEntity entity) {
		return Transaction.reconstitute(
				new TransactionId(entity.getId()),
				new AccountId(entity.getAccountId()),
				entity.getDate(),
				entity.getAmount(),
				entity.getDescription(),
				entity.getBalance(),
				new CategoryId(entity.getCategoryId()),
				entity.getCategoryAssignmentSource());
	}

}
