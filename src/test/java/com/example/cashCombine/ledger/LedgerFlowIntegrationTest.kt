package com.example.cashCombine.ledger

import com.example.cashCombine.ledger.accounts.AccountNotFoundException
import com.example.cashCombine.ledger.accounts.AccountService
import com.example.cashCombine.ledger.accounts.AccountType
import com.example.cashCombine.ledger.accounts.InMemoryAccountRepository
import com.example.cashCombine.ledger.categorisation.Category
import com.example.cashCombine.ledger.categorisation.CategoryAssignmentSource
import com.example.cashCombine.ledger.categorisation.CategoryRepository
import com.example.cashCombine.ledger.categorisation.ClassificationRule
import com.example.cashCombine.ledger.categorisation.ClassificationRuleRepository
import com.example.cashCombine.ledger.categorisation.ClassificationRuleService
import com.example.cashCombine.ledger.categorisation.InMemoryCategoryRepository
import com.example.cashCombine.ledger.categorisation.InMemoryClassificationRuleRepository
import com.example.cashCombine.ledger.categorisation.TransactionClassifier
import com.example.cashCombine.ledger.imports.CommBankCsvParser
import com.example.cashCombine.ledger.imports.ImportService
import com.example.cashCombine.ledger.imports.InMemoryImportBatchRepository
import com.example.cashCombine.ledger.imports.TransactionCsvParser
import com.example.cashCombine.ledger.transactions.CommBankFingerprintStrategy
import com.example.cashCombine.ledger.transactions.InMemoryTransactionRepository
import com.example.cashCombine.ledger.transactions.TransactionFingerprintStrategy
import com.example.cashCombine.ledger.transactions.TransactionRepository
import com.example.cashCombine.ledger.transactions.TransactionService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.EnumMap

class LedgerFlowIntegrationTest {

    private lateinit var accountService: AccountService
    private lateinit var importService: ImportService
    private lateinit var transactionService: TransactionService
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var uncategorised: Category
    private lateinit var groceries: Category
    private lateinit var streaming: Category
    private lateinit var dining: Category

    @BeforeEach
    fun setUp() {
        val accountRepository = InMemoryAccountRepository()
        transactionRepository = InMemoryTransactionRepository()
        val importBatchRepository = InMemoryImportBatchRepository()
        accountService = AccountService(accountRepository, transactionRepository, importBatchRepository)

        categoryRepository = InMemoryCategoryRepository()
        uncategorised = categoryRepository.save(Category.uncategorised())
        groceries = categoryRepository.save(Category.create("Groceries"))
        streaming = categoryRepository.save(Category.create("Streaming"))
        dining = categoryRepository.save(Category.create("Dining"))

        val ruleRepository: ClassificationRuleRepository = InMemoryClassificationRuleRepository()
        ruleRepository.save(ClassificationRule.create("WOOLWORTHS", groceries.id))
        ruleRepository.save(ClassificationRule.create("STREAMING", streaming.id))

        val classifier = TransactionClassifier(ruleRepository, uncategorised.id)
        val ruleService = ClassificationRuleService(ruleRepository, categoryRepository)
        transactionService = TransactionService(transactionRepository, categoryRepository, ruleService)

        val parsers = EnumMap<AccountType, TransactionCsvParser>(AccountType::class.java)
        parsers[AccountType.COMMBANK] = CommBankCsvParser()

        val fingerprintStrategies = EnumMap<AccountType, TransactionFingerprintStrategy>(AccountType::class.java)
        fingerprintStrategies[AccountType.COMMBANK] = CommBankFingerprintStrategy()

        importService = ImportService(
            accountRepository,
            transactionRepository,
            importBatchRepository,
            parsers,
            fingerprintStrategies,
            classifier)
    }

    @Test
    fun `create account import csv categorise override reimport then delete`() {
        val account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK)
        assertThat(account.hasImports()).isFalse()

        val firstImport = importService.importCsv(account.id(), sampleCsvStream())
        assertThat(firstImport.accepted).isEqualTo(7)
        assertThat(firstImport.duplicate).isEqualTo(1)
        assertThat(firstImport.rejected).isZero()

        val afterImport = accountService.getAccount(account.id())
        assertThat(afterImport.hasImports()).isTrue()
        assertThatThrownBy { afterImport.changeType(AccountType.COMMBANK) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("after imports")

        val transactions = transactionRepository.findByAccountId(account.id())
        assertThat(transactions).hasSize(7)
        assertThat(transactions).anySatisfy {
            assertThat(it.description()).contains("WOOLWORTHS")
            assertThat(it.categoryId()).isEqualTo(groceries.id)
        }
        assertThat(transactions).anySatisfy {
            assertThat(it.description()).contains("STREAMING")
            assertThat(it.categoryId()).isEqualTo(streaming.id)
        }
        val cafe = transactions.firstOrNull { it.description().contains("CAFE EXAMPLE") }
            ?: throw AssertionError("CAFE EXAMPLE not found")
        assertThat(cafe.categoryId()).isEqualTo(uncategorised.id)
        assertThat(cafe.categoryAssignmentSource()).isEqualTo(CategoryAssignmentSource.RULE)

        val overridden = transactionService.changeCategory(cafe.id(), dining.id)
        assertThat(overridden.categoryId()).isEqualTo(dining.id)
        assertThat(overridden.isManuallyCategorised()).isTrue()

        val secondImport = importService.importCsv(account.id(), sampleCsvStream())
        assertThat(secondImport.accepted).isZero()
        assertThat(secondImport.duplicate).isEqualTo(8)
        assertThat(secondImport.rejected).isZero()
        assertThat(transactionRepository.findByAccountId(account.id())).hasSize(7)
        assertThat(transactionService.getTransaction(cafe.id()).categoryId()).isEqualTo(dining.id)
        assertThat(transactionService.getTransaction(cafe.id()).isManuallyCategorised()).isTrue()

        accountService.deleteAccount(account.id())
        assertThatThrownBy { accountService.getAccount(account.id()) }
            .isInstanceOf(AccountNotFoundException::class.java)
        assertThat(transactionRepository.findByAccountId(account.id())).isEmpty()
    }

    private fun sampleCsvStream() =
        LedgerFlowIntegrationTest::class.java.getResourceAsStream("/csv/commbank-sample.csv")!!
}