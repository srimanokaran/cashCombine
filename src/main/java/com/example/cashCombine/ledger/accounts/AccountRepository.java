package com.example.cashCombine.ledger.accounts;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {

	Account save(Account account);

	Optional<Account> findById(AccountId id);

	Optional<Account> findByType(AccountType type);

	List<Account> findAll();

	void deleteById(AccountId id);

	boolean existsById(AccountId id);

}
