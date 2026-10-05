package com.example.cashCombine.ledger.imports

import com.example.cashCombine.ledger.accounts.Account
import com.example.cashCombine.ledger.accounts.AccountId
import com.example.cashCombine.ledger.accounts.AccountNotFoundException
import com.example.cashCombine.ledger.accounts.AccountService
import com.example.cashCombine.ledger.accounts.AccountType
import com.example.cashCombine.ledger.accounts.InMemoryAccountRepository
import com.example.cashCombine.ledger.categorisation.Category
import com.example.cashCombine.ledger.categorisation.CategoryRepository
import com.example.cashCombine.ledger.categorisation.ClassificationRule
import com.example.cashCombine.ledger.categorisation.ClassificationRuleRepository
import com.example.cashCombine.ledger.categorisation.InMemoryCategoryRepository
import com.example.cashCombine.ledger.categorisation.InMemoryClassificationRuleRepository
import com.example.cashCombine.ledger.categorisation.TransactionClassifier
import com.example.cashCombine.ledger.transactions.CommBankFingerprintStrategy
import com.example.cashCombine.ledger.transactions.InMemoryTransactionRepository
import com.example.cashCombine.ledger.transactions.NabCreditCardFingerprintStrategy
import com.example.cashCombine.ledger.transactions.Transaction
import com.example.cashCombine.ledger.transactions.TransactionFingerprintStrategy
import com.example.cashCombine.ledger.transactions.TransactionRepository
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.EnumMap
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ImportServiceTest {

    private lateinit var accountService: AccountService
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var groceries: Category
    private lateinit var uncategorised: Category
    private lateinit var importService: ImportService

    @BeforeEach
    fun setUp() {
        val accountRepository = InMemoryAccountRepository()
        transactionRepository = InMemoryTransactionRepository()
        val importBatchRepository = InMemoryImportBatchRepository()
        accountService = AccountService(accountRepository, transactionRepository, importBatchRepository)

        val categoryRepository = InMemoryCategoryRepository()
        uncategorised = categoryRepository.save(Category.uncategorised())
        groceries = categoryRepository.save(Category.create("Groceries"))

        val ruleRepository = InMemoryClassificationRuleRepository()
        ruleRepository.save(ClassificationRule.create("WOOLWORTHS", groceries.id))

        val classifier = TransactionClassifier(ruleRepository, uncategorised.id)

        val parsers = EnumMap<AccountType, TransactionCsvParser>(AccountType::class.java)
        parsers[AccountType.COMMBANK] = CommBankCsvParser()
        parsers[AccountType.NAB_CREDIT_CARD] = NabCreditCardCsvParser()

        val fingerprintStrategies = EnumMap<AccountType, TransactionFingerprintStrategy>(
            AccountType::class.java)
        fingerprintStrategies[AccountType.COMMBANK] = CommBankFingerprintStrategy()
        fingerprintStrategies[AccountType.NAB_CREDIT_CARD] = NabCreditCardFingerprintStrategy()

        importService = ImportService(
            accountRepository,
            transactionRepository,
            importBatchRepository,
            parsers,
            fingerprintStrategies,
            classifier
        )
    }

    @Test
    fun `imports sample fixture on first upload`() {
        val account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK)

        val result = importService.importCsv(account.id(), sampleCsvStream())

        assertThat(result.accepted).isEqualTo(7)
        assertThat(result.duplicate).isEqualTo(1)
        assertThat(result.rejected).isZero
        assertThat(accountService.getAccount(account.id()).hasImports()).isTrue
    }

    @Test
    fun `categorises matching rows and falls back to Uncategorised`() {
        val account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK)
        val csv = """
            10/07/2026,"-45.00","WOOLWORTHS 1234 FAKETOWN VIC AUS","+2455.00"
            09/07/2026,"-12.50","CAFE EXAMPLE BLEND FAKETOWN AUS","+2467.50"
        """.trimIndent()

        importService.importCsv(account.id(), stream(csv))

        assertThat(transactionRepository.findByAccountId(account.id()).map { it.categoryId() })
            .containsExactlyInAnyOrder(groceries.id, uncategorised.id)
    }

    @Test
    fun `reimporting same file is idempotent`() {
        val account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK)
        importService.importCsv(account.id(), sampleCsvStream())

        val result = importService.importCsv(account.id(), sampleCsvStream())

        assertThat(result.accepted).isZero
        assertThat(result.duplicate).isEqualTo(8)
        assertThat(result.rejected).isZero
    }

    @Test
    fun `treats different amount scales as duplicates`() {
        val account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK)
        val first = """
            10/07/2026,"-45.0","WOOLWORTHS 1234 FAKETOWN VIC AUS","+2455.0"
        """.trimIndent()
        val second = """
            10/07/2026,"-45.00","WOOLWORTHS 1234 FAKETOWN VIC AUS","+2455.00"
        """.trimIndent()

        val firstImport = importService.importCsv(account.id(), stream(first))
        val secondImport = importService.importCsv(account.id(), stream(second))

        assertThat(firstImport.accepted).isEqualTo(1)
        assertThat(secondImport.accepted).isZero
        assertThat(secondImport.duplicate).isEqualTo(1)
        assertThat(transactionRepository.findByAccountId(account.id())).hasSize(1)
    }

    @Test
    fun `throws when account not found`() {
        val missingAccountId = AccountId.generate()

        assertThatThrownBy { importService.importCsv(missingAccountId, sampleCsvStream()) }
            .isInstanceOf(AccountNotFoundException::class.java)
    }

    @Test
    fun `fails whole import when first row has wrong column count`() {
        val account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK)
        val csv = "10/07/2026,\"-45.00\",\"Only three columns\"\n"

        assertThatThrownBy { importService.importCsv(account.id(), stream(csv)) }
            .isInstanceOf(InvalidCsvFormatException::class.java)
            .hasMessageContaining("Expected 4 columns")

        assertThat(accountService.getAccount(account.id()).hasImports()).isFalse
    }

    @Test
    fun `rejects bad rows after first row and continues import`() {
        val account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK)
        val csv = """
            10/07/2026,"-45.00","WOOLWORTHS 1234 FAKETOWN VIC AUS","+2455.00"
            not-a-date,"-12.50","Bad row","+2467.50"
            09/07/2026,"-12.50","CAFE EXAMPLE BLEND FAKETOWN AUS","+2467.50"
        """.trimIndent()

        val result = importService.importCsv(account.id(), stream(csv))

        assertThat(result.accepted).isEqualTo(2)
        assertThat(result.duplicate).isZero
        assertThat(result.rejected).isEqualTo(1)
    }

    @Test
    fun `delete import removes linked transactions`() {
        val account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK)
        val first = """
            10/07/2026,"-45.00","WOOLWORTHS 1234 FAKETOWN VIC AUS","+2455.00"
        """.trimIndent()
        val second = """
            09/07/2026,"-12.50","CAFE EXAMPLE BLEND FAKETOWN AUS","+2467.50"
        """.trimIndent()

        val firstImport = importService.importCsv(account.id(), stream(first), "first.csv")
        val secondImport = importService.importCsv(account.id(), stream(second), "second.csv")

        assertThat(importService.listImports(account.id())).hasSize(2)
        assertThat(transactionRepository.findByAccountId(account.id())).hasSize(2)

        importService.deleteImport(account.id(), firstImport.id)

        assertThat(importService.listImports(account.id()).map { it.id })
            .containsExactly(secondImport.id)
        assertThat(transactionRepository.findByAccountId(account.id()).map { it.description() })
            .containsExactly("CAFE EXAMPLE BLEND FAKETOWN AUS")
        assertThat(accountService.getAccount(account.id()).hasImports()).isTrue

        importService.deleteImport(account.id(), secondImport.id)

        assertThat(importService.listImports(account.id())).isEmpty()
        assertThat(transactionRepository.findByAccountId(account.id())).isEmpty()
        assertThat(accountService.getAccount(account.id()).hasImports()).isFalse
    }

    @Test
    fun `imports Qantas Money sample for NAB credit card`() {
        val account = accountService.createAccount("NAB credit card", AccountType.NAB_CREDIT_CARD)

        val result = importService.importCsv(account.id(), qantasMoneySampleStream())

        assertThat(result.accepted).isEqualTo(6)
        assertThat(result.duplicate).isEqualTo(1)
        assertThat(result.rejected).isZero
        assertThat(transactionRepository.findByAccountId(account.id()).map { it.description() })
            .contains("WOOLWORTHS 1234 FAKETOWN", "BPAY PAYMENT - THANK YOU")
    }

    @Test
    fun `reimporting Qantas Money sample is idempotent`() {
        val account = accountService.createAccount("NAB credit card", AccountType.NAB_CREDIT_CARD)
        importService.importCsv(account.id(), qantasMoneySampleStream())

        val result = importService.importCsv(account.id(), qantasMoneySampleStream())

        assertThat(result.accepted).isZero
        assertThat(result.duplicate).isEqualTo(7)
        assertThat(result.rejected).isZero
    }

    companion object {
        private fun sampleCsvStream(): InputStream =
            ImportServiceTest::class.java.getResourceAsStream("/csv/commbank-sample.csv")!!

        private fun qantasMoneySampleStream(): InputStream =
            ImportServiceTest::class.java.getResourceAsStream("/csv/qantas-money-sample.csv")!!

        private fun stream(csv: String): InputStream =
            ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8))
    }
}