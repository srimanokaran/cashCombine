package com.example.cashCombine.config;

import com.example.cashCombine.ledger.accounts.AccountRepository;
import com.example.cashCombine.ledger.accounts.AccountService;
import com.example.cashCombine.ledger.accounts.AccountType;
import com.example.cashCombine.ledger.categorisation.Category;
import com.example.cashCombine.ledger.categorisation.CategoryId;
import com.example.cashCombine.ledger.categorisation.CategoryReanalysisService;
import com.example.cashCombine.ledger.categorisation.CategoryRepository;
import com.example.cashCombine.ledger.categorisation.CategoryService;
import com.example.cashCombine.ledger.categorisation.ClassificationRuleRepository;
import com.example.cashCombine.ledger.categorisation.ClassificationRuleService;
import com.example.cashCombine.ledger.categorisation.TransactionClassifier;
import com.example.cashCombine.ledger.dashboard.DashboardService;
import com.example.cashCombine.ledger.imports.CommBankCsvParser;
import com.example.cashCombine.ledger.imports.ImportBatchRepository;
import com.example.cashCombine.ledger.imports.ImportService;
import com.example.cashCombine.ledger.imports.NabCreditCardCsvParser;
import com.example.cashCombine.ledger.imports.TransactionCsvParser;
import com.example.cashCombine.ledger.transactions.CommBankFingerprintStrategy;
import com.example.cashCombine.ledger.transactions.NabCreditCardFingerprintStrategy;
import com.example.cashCombine.ledger.transactions.TransactionFingerprintStrategy;
import com.example.cashCombine.ledger.transactions.TransactionRepository;
import com.example.cashCombine.ledger.transactions.TransactionService;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class LedgerConfig {

	@Bean
	AccountService accountService(
			AccountRepository accountRepository,
			TransactionRepository transactionRepository,
			ImportBatchRepository importBatchRepository) {
		return new AccountService(accountRepository, transactionRepository, importBatchRepository);
	}

	@Bean
	CategoryService categoryService(
			CategoryRepository categoryRepository,
			ClassificationRuleRepository ruleRepository,
			TransactionRepository transactionRepository) {
		return new CategoryService(categoryRepository, ruleRepository, transactionRepository);
	}

	@Bean
	ClassificationRuleService classificationRuleService(
			ClassificationRuleRepository ruleRepository, CategoryRepository categoryRepository) {
		return new ClassificationRuleService(ruleRepository, categoryRepository);
	}

	@Bean
	TransactionService transactionService(
			TransactionRepository transactionRepository,
			CategoryRepository categoryRepository,
			ClassificationRuleService classificationRuleService) {
		return new TransactionService(transactionRepository, categoryRepository, classificationRuleService);
	}

	@Bean
	CategoryReanalysisService categoryReanalysisService(
			TransactionRepository transactionRepository, TransactionClassifier transactionClassifier) {
		return new CategoryReanalysisService(transactionRepository, transactionClassifier);
	}

	@Bean
	DashboardService dashboardService(
			TransactionRepository transactionRepository,
			CategoryRepository categoryRepository,
			AccountRepository accountRepository) {
		return new DashboardService(transactionRepository, categoryRepository, accountRepository);
	}

	@Bean
	CategoryId uncategorisedCategoryId(CategoryRepository categoryRepository) {
		return categoryRepository
				.findByName(Category.UNCATEGORISED_NAME)
				.orElseGet(() -> categoryRepository.save(Category.uncategorised()))
				.id();
	}

	@Bean
	TransactionClassifier transactionClassifier(
			ClassificationRuleRepository ruleRepository, CategoryId uncategorisedCategoryId) {
		return new TransactionClassifier(ruleRepository, uncategorisedCategoryId);
	}

	@Bean
	Map<AccountType, TransactionCsvParser> transactionCsvParsers() {
		Map<AccountType, TransactionCsvParser> parsers = new EnumMap<>(AccountType.class);
		parsers.put(AccountType.COMMBANK, new CommBankCsvParser());
		parsers.put(AccountType.NAB_CREDIT_CARD, new NabCreditCardCsvParser());
		return parsers;
	}

	@Bean
	Map<AccountType, TransactionFingerprintStrategy> transactionFingerprintStrategies() {
		Map<AccountType, TransactionFingerprintStrategy> strategies = new EnumMap<>(AccountType.class);
		strategies.put(AccountType.COMMBANK, new CommBankFingerprintStrategy());
		strategies.put(AccountType.NAB_CREDIT_CARD, new NabCreditCardFingerprintStrategy());
		return strategies;
	}

	@Bean
	ImportService importService(
			AccountRepository accountRepository,
			TransactionRepository transactionRepository,
			ImportBatchRepository importBatchRepository,
			Map<AccountType, TransactionCsvParser> transactionCsvParsers,
			Map<AccountType, TransactionFingerprintStrategy> transactionFingerprintStrategies,
			TransactionClassifier transactionClassifier) {
		return new ImportService(
				accountRepository,
				transactionRepository,
				importBatchRepository,
				transactionCsvParsers,
				transactionFingerprintStrategies,
				transactionClassifier);
	}

}
