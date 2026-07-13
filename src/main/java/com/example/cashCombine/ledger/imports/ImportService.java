package com.example.cashCombine.ledger.imports;

import com.example.cashCombine.ledger.accounts.Account;
import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.accounts.AccountNotFoundException;
import com.example.cashCombine.ledger.accounts.AccountRepository;
import com.example.cashCombine.ledger.accounts.AccountType;
import com.example.cashCombine.ledger.transactions.CommBankFingerprintStrategy;
import com.example.cashCombine.ledger.transactions.Transaction;
import com.example.cashCombine.ledger.transactions.TransactionFingerprint;
import com.example.cashCombine.ledger.transactions.TransactionFingerprintStrategy;
import com.example.cashCombine.ledger.transactions.TransactionRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ImportService {

	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;
	// Currently hardcoded to CommBank, but could be extended to other banks
	private final CommBankCsvParser parser;
	// In memory map of fingerprint strategies for each account type
	private final Map<AccountType, TransactionFingerprintStrategy> fingerprintStrategies;

	public ImportService(
			AccountRepository accountRepository,
			TransactionRepository transactionRepository,
			CommBankCsvParser parser) {
		this(accountRepository, transactionRepository, parser, defaultFingerprintStrategies());
	}

	ImportService(
			AccountRepository accountRepository,
			TransactionRepository transactionRepository,
			CommBankCsvParser parser,
			Map<AccountType, TransactionFingerprintStrategy> fingerprintStrategies) {
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
		this.parser = parser;
		this.fingerprintStrategies = fingerprintStrategies;
	}

	/**
	 * Imports CSV transactions into an account (idempotent).
	 *
	 * Input:
	 * - accountId: target account (must exist; type selects fingerprint rules)
	 * - input: CSV bytes (CommBank: no header; date, amount, description, balance per line)
	 *
	 * Output ImportResult:
	 * - accepted: new rows saved
	 * - duplicate: already seen for this account (skipped, not updated)
	 * - rejected: bad rows after the first data row (skipped; import continues)
	 *
	 * Failures:
	 * - missing account: AccountNotFoundException
	 * - first data row unparseable: InvalidCsvFormatException (whole import fails)
	 */
	public ImportResult importCsv(AccountId accountId, InputStream input) throws IOException {
		Account account = accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
		TransactionFingerprintStrategy fingerprintStrategy = fingerprintStrategies.get(account.type());
		if (fingerprintStrategy == null) {
			throw new IllegalArgumentException("Account type" + account.type() + " could not be found");
		}

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

				try {
					ParsedTransactionRow row = parser.parseLine(line);
					TransactionFingerprint fingerprint = fingerprintStrategy.fingerprint(row);
					if (transactionRepository.existsByAccountAndFingerprint(accountId, fingerprint)
							|| acceptedThisImport.contains(fingerprint)) {
						duplicate++;
					}
					else {
						Transaction transaction = Transaction.create(accountId, row);
						transactionRepository.save(transaction);
						acceptedThisImport.add(fingerprint);
						accepted++;
					}
				}
				catch (InvalidCsvRowException ex) {
					if (firstDataRow) {
						throw new InvalidCsvFormatException(ex.getMessage(), ex);
					}
					rejected++;
				}

				firstDataRow = false;
			}
		}

		if (accepted > 0 || duplicate > 0) {
			account.markAsImported();
			accountRepository.save(account);
		}

		return new ImportResult(accepted, duplicate, rejected);
	}

	private static Map<AccountType, TransactionFingerprintStrategy> defaultFingerprintStrategies() {
		Map<AccountType, TransactionFingerprintStrategy> strategies = new EnumMap<>(AccountType.class);
		strategies.put(AccountType.COMMBANK, new CommBankFingerprintStrategy());
		return strategies;
	}

}
