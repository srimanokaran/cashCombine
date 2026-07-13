package com.example.cashCombine.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.cashCombine.ledger.accounts.Account;
import com.example.cashCombine.ledger.accounts.AccountNotFoundException;
import com.example.cashCombine.ledger.accounts.AccountService;
import com.example.cashCombine.ledger.accounts.AccountType;
import com.example.cashCombine.ledger.accounts.InMemoryAccountRepository;
import com.example.cashCombine.ledger.categorisation.Category;
import com.example.cashCombine.ledger.categorisation.CategoryRepository;
import com.example.cashCombine.ledger.categorisation.ClassificationRule;
import com.example.cashCombine.ledger.categorisation.ClassificationRuleRepository;
import com.example.cashCombine.ledger.categorisation.InMemoryCategoryRepository;
import com.example.cashCombine.ledger.categorisation.InMemoryClassificationRuleRepository;
import com.example.cashCombine.ledger.categorisation.TransactionClassifier;
import com.example.cashCombine.ledger.imports.CommBankCsvParser;
import com.example.cashCombine.ledger.imports.ImportResult;
import com.example.cashCombine.ledger.imports.ImportService;
import com.example.cashCombine.ledger.imports.TransactionCsvParser;
import com.example.cashCombine.ledger.transactions.CommBankFingerprintStrategy;
import com.example.cashCombine.ledger.transactions.InMemoryTransactionRepository;
import com.example.cashCombine.ledger.transactions.Transaction;
import com.example.cashCombine.ledger.transactions.TransactionFingerprintStrategy;
import com.example.cashCombine.ledger.transactions.TransactionRepository;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end flow across accounts, import, categorisation, and delete.
 * Uses in-memory repos (domain integration). Spring/JPA persistence comes later.
 */
class LedgerFlowIntegrationTest {

	private AccountService accountService;
	private ImportService importService;
	private TransactionRepository transactionRepository;
	private Category uncategorised;
	private Category groceries;
	private Category streaming;

	@BeforeEach
	void setUp() {
		var accountRepository = new InMemoryAccountRepository();
		transactionRepository = new InMemoryTransactionRepository();
		accountService = new AccountService(accountRepository, transactionRepository);

		CategoryRepository categoryRepository = new InMemoryCategoryRepository();
		uncategorised = categoryRepository.save(Category.uncategorised());
		groceries = categoryRepository.save(Category.create("Groceries"));
		streaming = categoryRepository.save(Category.create("Streaming"));

		ClassificationRuleRepository ruleRepository = new InMemoryClassificationRuleRepository();
		ruleRepository.save(ClassificationRule.create("WOOLWORTHS", groceries.id()));
		ruleRepository.save(ClassificationRule.create("STREAMING", streaming.id()));

		TransactionClassifier classifier = new TransactionClassifier(ruleRepository, uncategorised.id());

		Map<AccountType, TransactionCsvParser> parsers = new EnumMap<>(AccountType.class);
		parsers.put(AccountType.COMMBANK, new CommBankCsvParser());

		Map<AccountType, TransactionFingerprintStrategy> fingerprintStrategies = new EnumMap<>(AccountType.class);
		fingerprintStrategies.put(AccountType.COMMBANK, new CommBankFingerprintStrategy());

		importService = new ImportService(
				accountRepository,
				transactionRepository,
				parsers,
				fingerprintStrategies,
				classifier);
	}

	@Test
	void createAccount_importCsv_categorise_reimport_thenDelete() throws Exception {
		// 1. Create account
		Account account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK);
		assertThat(account.hasImports()).isFalse();

		// 2. First import (sample has 8 rows; 2 identical → 7 accepted, 1 duplicate)
		ImportResult firstImport = importService.importCsv(account.id(), sampleCsvStream());
		assertThat(firstImport.accepted()).isEqualTo(7);
		assertThat(firstImport.duplicate()).isEqualTo(1);
		assertThat(firstImport.rejected()).isZero();

		Account afterImport = accountService.getAccount(account.id());
		assertThat(afterImport.hasImports()).isTrue();
		assertThatThrownBy(() -> afterImport.changeType(AccountType.COMMBANK))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("after imports");

		// 3. Categorisation applied on accepted rows
		List<Transaction> transactions = transactionRepository.findByAccountId(account.id());
		assertThat(transactions).hasSize(7);
		assertThat(transactions).anySatisfy(tx -> {
			assertThat(tx.description()).contains("WOOLWORTHS");
			assertThat(tx.categoryId()).isEqualTo(groceries.id());
		});
		assertThat(transactions).anySatisfy(tx -> {
			assertThat(tx.description()).contains("STREAMING");
			assertThat(tx.categoryId()).isEqualTo(streaming.id());
		});
		assertThat(transactions).anySatisfy(tx -> {
			assertThat(tx.description()).contains("CAFE EXAMPLE");
			assertThat(tx.categoryId()).isEqualTo(uncategorised.id());
		});

		// 4. Re-import same file — idempotent
		ImportResult secondImport = importService.importCsv(account.id(), sampleCsvStream());
		assertThat(secondImport.accepted()).isZero();
		assertThat(secondImport.duplicate()).isEqualTo(8);
		assertThat(secondImport.rejected()).isZero();
		assertThat(transactionRepository.findByAccountId(account.id())).hasSize(7);

		// 5. Delete account cascades transactions
		accountService.deleteAccount(account.id());
		assertThatThrownBy(() -> accountService.getAccount(account.id()))
				.isInstanceOf(AccountNotFoundException.class);
		assertThat(transactionRepository.findByAccountId(account.id())).isEmpty();
	}

	private static InputStream sampleCsvStream() {
		return LedgerFlowIntegrationTest.class.getResourceAsStream("/csv/commbank-sample.csv");
	}

}
