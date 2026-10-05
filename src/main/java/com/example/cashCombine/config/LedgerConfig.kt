package com.example.cashCombine.config

import com.example.cashCombine.ledger.accounts.AccountRepository
import com.example.cashCombine.ledger.accounts.AccountService
import com.example.cashCombine.ledger.accounts.AccountType
import com.example.cashCombine.ledger.categorisation.Category
import com.example.cashCombine.ledger.categorisation.CategoryId
import com.example.cashCombine.ledger.categorisation.CategoryReanalysisService
import com.example.cashCombine.ledger.categorisation.CategoryRepository
import com.example.cashCombine.ledger.categorisation.CategoryService
import com.example.cashCombine.ledger.categorisation.ClassificationRuleRepository
import com.example.cashCombine.ledger.categorisation.ClassificationRuleService
import com.example.cashCombine.ledger.categorisation.TransactionClassifier
import com.example.cashCombine.ledger.dashboard.DashboardService
import com.example.cashCombine.ledger.imports.CommBankCsvParser
import com.example.cashCombine.ledger.imports.ImportBatchRepository
import com.example.cashCombine.ledger.imports.ImportService
import com.example.cashCombine.ledger.imports.NabCreditCardCsvParser
import com.example.cashCombine.ledger.imports.TransactionCsvParser
import com.example.cashCombine.ledger.transactions.CommBankFingerprintStrategy
import com.example.cashCombine.ledger.transactions.NabCreditCardFingerprintStrategy
import com.example.cashCombine.ledger.transactions.TransactionFingerprintStrategy
import com.example.cashCombine.ledger.transactions.TransactionRepository
import com.example.cashCombine.ledger.transactions.TransactionService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.util.EnumMap

@Configuration
@EnableTransactionManagement
class LedgerConfig {

    @Bean
    fun accountService(
        accountRepository: AccountRepository,
        transactionRepository: TransactionRepository,
        importBatchRepository: ImportBatchRepository,
    ): AccountService {
        return AccountService(accountRepository, transactionRepository, importBatchRepository)
    }

    @Bean
    fun categoryService(
        categoryRepository: CategoryRepository,
        ruleRepository: ClassificationRuleRepository,
        transactionRepository: TransactionRepository,
    ): CategoryService {
        return CategoryService(categoryRepository, ruleRepository, transactionRepository)
    }

    @Bean
    fun classificationRuleService(
        ruleRepository: ClassificationRuleRepository,
        categoryRepository: CategoryRepository,
    ): ClassificationRuleService {
        return ClassificationRuleService(ruleRepository, categoryRepository)
    }

    @Bean
    fun transactionService(
        transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository,
        classificationRuleService: ClassificationRuleService,
    ): TransactionService {
        return TransactionService(transactionRepository, categoryRepository, classificationRuleService)
    }

    @Bean
    fun categoryReanalysisService(
        transactionRepository: TransactionRepository,
        transactionClassifier: TransactionClassifier,
    ): CategoryReanalysisService {
        return CategoryReanalysisService(transactionRepository, transactionClassifier)
    }

    @Bean
    fun dashboardService(
        transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository,
        accountRepository: AccountRepository,
    ): DashboardService {
        return DashboardService(transactionRepository, categoryRepository, accountRepository)
    }

    @Bean
    fun uncategorisedCategoryId(categoryRepository: CategoryRepository): CategoryId {
        val category = categoryRepository.findByName(Category.UNCATEGORISED_NAME)
            ?: categoryRepository.save(Category.uncategorised())
        return category.id
    }

    @Bean
    fun transactionClassifier(
        ruleRepository: ClassificationRuleRepository,
        uncategorisedCategoryId: CategoryId,
    ): TransactionClassifier {
        return TransactionClassifier(ruleRepository, uncategorisedCategoryId)
    }

    @Bean
    fun transactionCsvParsers(): Map<AccountType, TransactionCsvParser> {
        val parsers = EnumMap<AccountType, TransactionCsvParser>(AccountType::class.java)
        parsers[AccountType.COMMBANK] = CommBankCsvParser()
        parsers[AccountType.NAB_CREDIT_CARD] = NabCreditCardCsvParser()
        return parsers
    }

    @Bean
    fun transactionFingerprintStrategies(): Map<AccountType, TransactionFingerprintStrategy> {
        val strategies = EnumMap<AccountType, TransactionFingerprintStrategy>(AccountType::class.java)
        strategies[AccountType.COMMBANK] = CommBankFingerprintStrategy()
        strategies[AccountType.NAB_CREDIT_CARD] = NabCreditCardFingerprintStrategy()
        return strategies
    }

    @Bean
    fun importService(
        accountRepository: AccountRepository,
        transactionRepository: TransactionRepository,
        importBatchRepository: ImportBatchRepository,
        transactionCsvParsers: Map<AccountType, TransactionCsvParser>,
        transactionFingerprintStrategies: Map<AccountType, TransactionFingerprintStrategy>,
        transactionClassifier: TransactionClassifier,
    ): ImportService {
        return ImportService(
            accountRepository,
            transactionRepository,
            importBatchRepository,
            transactionCsvParsers,
            transactionFingerprintStrategies,
            transactionClassifier)
    }
}