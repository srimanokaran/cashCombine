package com.example.cashCombine.ledger.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.cashCombine.ledger.accounts.Account;
import com.example.cashCombine.ledger.accounts.AccountId;
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
import com.example.cashCombine.ledger.transactions.CommBankFingerprintStrategy;
import com.example.cashCombine.ledger.transactions.InMemoryTransactionRepository;
import com.example.cashCombine.ledger.transactions.NabCreditCardFingerprintStrategy;
import com.example.cashCombine.ledger.transactions.Transaction;
import com.example.cashCombine.ledger.transactions.TransactionFingerprintStrategy;
import com.example.cashCombine.ledger.transactions.TransactionRepository;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImportServiceTest {

	private AccountService accountService;
	private TransactionRepository transactionRepository;
	private Category groceries;
	private Category uncategorised;
	private ImportService importService;

	@BeforeEach
	void setUp() {
		var accountRepository = new InMemoryAccountRepository();
		transactionRepository = new InMemoryTransactionRepository();
		var importBatchRepository = new InMemoryImportBatchRepository();
		accountService = new AccountService(accountRepository, transactionRepository, importBatchRepository);

		CategoryRepository categoryRepository = new InMemoryCategoryRepository();
		uncategorised = categoryRepository.save(Category.uncategorised());
		groceries = categoryRepository.save(Category.create("Groceries"));

		ClassificationRuleRepository ruleRepository = new InMemoryClassificationRuleRepository();
		ruleRepository.save(ClassificationRule.create("WOOLWORTHS", groceries.id()));

		TransactionClassifier classifier = new TransactionClassifier(ruleRepository, uncategorised.id());

		Map<AccountType, TransactionCsvParser> parsers = new EnumMap<>(AccountType.class);
		parsers.put(AccountType.COMMBANK, new CommBankCsvParser());
		parsers.put(AccountType.NAB_CREDIT_CARD, new NabCreditCardCsvParser());

		Map<AccountType, TransactionFingerprintStrategy> fingerprintStrategies = new EnumMap<>(AccountType.class);
		fingerprintStrategies.put(AccountType.COMMBANK, new CommBankFingerprintStrategy());
		fingerprintStrategies.put(AccountType.NAB_CREDIT_CARD, new NabCreditCardFingerprintStrategy());

		importService = new ImportService(
				accountRepository,
				transactionRepository,
				importBatchRepository,
				parsers,
				fingerprintStrategies,
				classifier);
	}

	@Test
	void importsSampleFixtureOnFirstUpload() throws Exception {
		Account account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK);

		ImportResult result = importService.importCsv(account.id(), sampleCsvStream());

		assertThat(result.accepted()).isEqualTo(7);
		assertThat(result.duplicate()).isEqualTo(1);
		assertThat(result.rejected()).isZero();
		assertThat(accountService.getAccount(account.id()).hasImports()).isTrue();
	}

	@Test
	void categorisesMatchingRowsAndFallsBackToUncategorised() throws Exception {
		Account account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK);
		String csv = """
				10/07/2026,"-45.00","WOOLWORTHS 1234 FAKETOWN VIC AUS","+2455.00"
				09/07/2026,"-12.50","CAFE EXAMPLE BLEND FAKETOWN AUS","+2467.50"
				""";

		importService.importCsv(account.id(), stream(csv));

		assertThat(transactionRepository.findByAccountId(account.id()))
				.extracting(Transaction::categoryId)
				.containsExactlyInAnyOrder(groceries.id(), uncategorised.id());
	}

	@Test
	void reimportingSameFileIsIdempotent() throws Exception {
		Account account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK);
		importService.importCsv(account.id(), sampleCsvStream());

		ImportResult result = importService.importCsv(account.id(), sampleCsvStream());

		assertThat(result.accepted()).isZero();
		assertThat(result.duplicate()).isEqualTo(8);
		assertThat(result.rejected()).isZero();
	}

	@Test
	void treatsDifferentAmountScalesAsDuplicates() throws Exception {
		Account account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK);
		String first = """
				10/07/2026,"-45.0","WOOLWORTHS 1234 FAKETOWN VIC AUS","+2455.0"
				""";
		String second = """
				10/07/2026,"-45.00","WOOLWORTHS 1234 FAKETOWN VIC AUS","+2455.00"
				""";

		ImportResult firstImport = importService.importCsv(account.id(), stream(first));
		ImportResult secondImport = importService.importCsv(account.id(), stream(second));

		assertThat(firstImport.accepted()).isEqualTo(1);
		assertThat(secondImport.accepted()).isZero();
		assertThat(secondImport.duplicate()).isEqualTo(1);
		assertThat(transactionRepository.findByAccountId(account.id())).hasSize(1);
	}

	@Test
	void throwsWhenAccountNotFound() {
		var missingAccountId = AccountId.generate();

		assertThatThrownBy(() -> importService.importCsv(missingAccountId, sampleCsvStream()))
				.isInstanceOf(AccountNotFoundException.class);
	}

	@Test
	void failsWholeImportWhenFirstRowHasWrongColumnCount() throws Exception {
		Account account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK);
		String csv = "10/07/2026,\"-45.00\",\"Only three columns\"\n";

		assertThatThrownBy(() -> importService.importCsv(account.id(), stream(csv)))
				.isInstanceOf(InvalidCsvFormatException.class)
				.hasMessageContaining("Expected 4 columns");

		assertThat(accountService.getAccount(account.id()).hasImports()).isFalse();
	}

	@Test
	void rejectsBadRowsAfterFirstRowAndContinuesImport() throws Exception {
		Account account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK);
		String csv = """
				10/07/2026,"-45.00","WOOLWORTHS 1234 FAKETOWN VIC AUS","+2455.00"
				not-a-date,"-12.50","Bad row","+2467.50"
				09/07/2026,"-12.50","CAFE EXAMPLE BLEND FAKETOWN AUS","+2467.50"
				""";

		ImportResult result = importService.importCsv(account.id(), stream(csv));

		assertThat(result.accepted()).isEqualTo(2);
		assertThat(result.duplicate()).isZero();
		assertThat(result.rejected()).isEqualTo(1);
	}

	@Test
	void deleteImportRemovesLinkedTransactions() throws Exception {
		Account account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK);
		String first = """
				10/07/2026,"-45.00","WOOLWORTHS 1234 FAKETOWN VIC AUS","+2455.00"
				""";
		String second = """
				09/07/2026,"-12.50","CAFE EXAMPLE BLEND FAKETOWN AUS","+2467.50"
				""";

		ImportResult firstImport = importService.importCsv(account.id(), stream(first), "first.csv");
		ImportResult secondImport = importService.importCsv(account.id(), stream(second), "second.csv");

		assertThat(importService.listImports(account.id())).hasSize(2);
		assertThat(transactionRepository.findByAccountId(account.id())).hasSize(2);

		importService.deleteImport(account.id(), firstImport.id());

		assertThat(importService.listImports(account.id()))
				.extracting(batch -> batch.id())
				.containsExactly(secondImport.id());
		assertThat(transactionRepository.findByAccountId(account.id()))
				.extracting(Transaction::description)
				.containsExactly("CAFE EXAMPLE BLEND FAKETOWN AUS");
		assertThat(accountService.getAccount(account.id()).hasImports()).isTrue();

		importService.deleteImport(account.id(), secondImport.id());

		assertThat(importService.listImports(account.id())).isEmpty();
		assertThat(transactionRepository.findByAccountId(account.id())).isEmpty();
		assertThat(accountService.getAccount(account.id()).hasImports()).isFalse();
	}

	@Test
	void importsQantasMoneySampleForNabCreditCard() throws Exception {
		Account account = accountService.createAccount("NAB credit card", AccountType.NAB_CREDIT_CARD);

		ImportResult result = importService.importCsv(account.id(), qantasMoneySampleStream());

		// 7 rows in fixture; two identical Qantas Airways lines → 6 accepted, 1 duplicate
		assertThat(result.accepted()).isEqualTo(6);
		assertThat(result.duplicate()).isEqualTo(1);
		assertThat(result.rejected()).isZero();
		assertThat(transactionRepository.findByAccountId(account.id()))
				.extracting(Transaction::description)
				.contains("WOOLWORTHS 1234 FAKETOWN", "BPAY PAYMENT - THANK YOU");
	}

	@Test
	void reimportingQantasMoneySampleIsIdempotent() throws Exception {
		Account account = accountService.createAccount("NAB credit card", AccountType.NAB_CREDIT_CARD);
		importService.importCsv(account.id(), qantasMoneySampleStream());

		ImportResult result = importService.importCsv(account.id(), qantasMoneySampleStream());

		assertThat(result.accepted()).isZero();
		assertThat(result.duplicate()).isEqualTo(7);
		assertThat(result.rejected()).isZero();
	}

	private static InputStream sampleCsvStream() {
		return ImportServiceTest.class.getResourceAsStream("/csv/commbank-sample.csv");
	}

	private static InputStream qantasMoneySampleStream() {
		return ImportServiceTest.class.getResourceAsStream("/csv/qantas-money-sample.csv");
	}

	private static InputStream stream(String csv) {
		return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
	}

}
