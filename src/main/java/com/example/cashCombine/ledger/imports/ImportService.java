package com.example.cashCombine.ledger.imports;

import com.example.cashCombine.ledger.accounts.Account;
import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.accounts.AccountNotFoundException;
import com.example.cashCombine.ledger.accounts.AccountRepository;
import com.example.cashCombine.ledger.accounts.AccountType;
import com.example.cashCombine.ledger.categorisation.CategoryId;
import com.example.cashCombine.ledger.categorisation.TransactionClassifier;
import com.example.cashCombine.ledger.transactions.Transaction;
import com.example.cashCombine.ledger.transactions.TransactionFingerprint;
import com.example.cashCombine.ledger.transactions.TransactionFingerprintStrategy;
import com.example.cashCombine.ledger.transactions.TransactionRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ImportService {

	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;
	private final Map<AccountType, TransactionCsvParser> parsers;
	private final Map<AccountType, TransactionFingerprintStrategy> fingerprintStrategies;
	private final TransactionClassifier classifier;

	public ImportService(
			AccountRepository accountRepository,
			TransactionRepository transactionRepository,
			Map<AccountType, TransactionCsvParser> parsers,
			Map<AccountType, TransactionFingerprintStrategy> fingerprintStrategies,
			TransactionClassifier classifier) {
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
		this.parsers = parsers;
		this.fingerprintStrategies = fingerprintStrategies;
		this.classifier = classifier;
	}

	/**
	 * Imports CSV transactions into an account (idempotent).
	 *
	 * Input:
	 * - accountId: target account (must exist; type selects parser and fingerprint rules)
	 * - input: CSV bytes (format depends on account type)
	 *
	 * Output ImportResult:
	 * - accepted: new rows saved (with category from rules or Uncategorised)
	 * - duplicate: already seen for this account (skipped, not updated)
	 * - rejected: bad rows after the first data row (skipped; import continues)
	 *
	 * Failures:
	 * - missing account: AccountNotFoundException
	 * - first data row unparseable: InvalidCsvFormatException (whole import fails)
	 */
	public ImportResult importCsv(AccountId accountId, InputStream input) throws IOException {
		Account account = requireAccount(accountId);
		TransactionCsvParser parser = requireParser(account.type());
		TransactionFingerprintStrategy fingerprintStrategy = requireFingerprintStrategy(account.type());

		ImportResult result = processRows(accountId, input, parser, fingerprintStrategy);
		markImportedIfNeeded(account, result);
		return result;
	}

	private Account requireAccount(AccountId accountId) {
		return accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
	}

	private TransactionCsvParser requireParser(AccountType accountType) {
		TransactionCsvParser parser = parsers.get(accountType);
		if (parser == null) {
			throw new IllegalArgumentException("No CSV parser registered for account type: " + accountType);
		}
		return parser;
	}

	private TransactionFingerprintStrategy requireFingerprintStrategy(AccountType accountType) {
		TransactionFingerprintStrategy strategy = fingerprintStrategies.get(accountType);
		if (strategy == null) {
			throw new IllegalArgumentException("No fingerprint strategy registered for account type: " + accountType);
		}
		return strategy;
	}

	private ImportResult processRows(
			AccountId accountId,
			InputStream input,
			TransactionCsvParser parser,
			TransactionFingerprintStrategy fingerprintStrategy) throws IOException {
		int accepted = 0;
		int duplicate = 0;
		int rejected = 0;
		Set<TransactionFingerprint> acceptedThisImport = new HashSet<>();
		boolean firstDataRow = true;

		try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}

				RowOutcome outcome = processRow(accountId, line, parser, fingerprintStrategy, acceptedThisImport, firstDataRow);
				switch (outcome) {
					case ACCEPTED -> accepted++;
					case DUPLICATE -> duplicate++;
					case REJECTED -> rejected++;
				}
				firstDataRow = false;
			}
		}

		return new ImportResult(accepted, duplicate, rejected);
	}

	private RowOutcome processRow(
			AccountId accountId,
			String line,
			TransactionCsvParser parser,
			TransactionFingerprintStrategy fingerprintStrategy,
			Set<TransactionFingerprint> acceptedThisImport,
			boolean firstDataRow) {
		try {
			ParsedTransactionRow row = parser.parseLine(line);
			TransactionFingerprint fingerprint = fingerprintStrategy.fingerprint(row);

			if (transactionRepository.existsByAccountAndFingerprint(accountId, fingerprint)
					|| acceptedThisImport.contains(fingerprint)) {
				return RowOutcome.DUPLICATE;
			}

			CategoryId categoryId = classifier.classify(row.description());
			transactionRepository.save(Transaction.create(accountId, row, categoryId));
			acceptedThisImport.add(fingerprint);
			return RowOutcome.ACCEPTED;
		}
		catch (InvalidCsvRowException ex) {
			if (firstDataRow) {
				throw new InvalidCsvFormatException(ex.getMessage(), ex);
			}
			return RowOutcome.REJECTED;
		}
	}

	private void markImportedIfNeeded(Account account, ImportResult result) {
		if (result.accepted() > 0 || result.duplicate() > 0) {
			account.markAsImported();
			accountRepository.save(account);
		}
	}

	private enum RowOutcome {
		ACCEPTED,
		DUPLICATE,
		REJECTED
	}

}
