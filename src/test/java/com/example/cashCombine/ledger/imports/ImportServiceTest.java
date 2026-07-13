package com.example.cashCombine.ledger.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.cashCombine.ledger.accounts.Account;
import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.accounts.AccountNotFoundException;
import com.example.cashCombine.ledger.accounts.AccountService;
import com.example.cashCombine.ledger.accounts.AccountType;
import com.example.cashCombine.ledger.accounts.InMemoryAccountRepository;
import com.example.cashCombine.ledger.transactions.InMemoryTransactionRepository;
import com.example.cashCombine.ledger.transactions.TransactionRepository;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImportServiceTest {

	private AccountService accountService;
	private TransactionRepository transactionRepository;
	private ImportService importService;

	@BeforeEach
	void setUp() {
		var accountRepository = new InMemoryAccountRepository();
		transactionRepository = new InMemoryTransactionRepository();
		accountService = new AccountService(accountRepository, transactionRepository);
		importService = new ImportService(accountRepository, transactionRepository, new CommBankCsvParser());
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
	void reimportingSameFileIsIdempotent() throws Exception {
		Account account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK);
		importService.importCsv(account.id(), sampleCsvStream());

		ImportResult result = importService.importCsv(account.id(), sampleCsvStream());

		assertThat(result.accepted()).isZero();
		assertThat(result.duplicate()).isEqualTo(8);
		assertThat(result.rejected()).isZero();
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

	private static InputStream sampleCsvStream() {
		return ImportServiceTest.class.getResourceAsStream("/csv/commbank-sample.csv");
	}

	private static InputStream stream(String csv) {
		return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
	}

}
