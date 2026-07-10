package com.example.cashCombine.ledger.accounts;

import java.util.Optional;

public interface AccountRepository {

	Account save(Account account);

	Optional<Account> findById(AccountId id);

	void deleteById(AccountId id);

	boolean existsById(AccountId id);

}
