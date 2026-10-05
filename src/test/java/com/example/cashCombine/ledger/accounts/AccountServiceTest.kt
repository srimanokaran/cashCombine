package com.example.cashCombine.ledger.accounts

import com.example.cashCombine.ledger.categorisation.Category
import com.example.cashCombine.ledger.imports.InMemoryImportBatchRepository
import com.example.cashCombine.ledger.imports.ParsedTransactionRow
import com.example.cashCombine.ledger.transactions.InMemoryTransactionRepository
import com.example.cashCombine.ledger.transactions.Transaction
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class AccountServiceTest {

    private lateinit var accountService: AccountService
    private lateinit var transactionRepository: InMemoryTransactionRepository

    @BeforeEach
    fun setUp() {
        val accountRepository = InMemoryAccountRepository()
        transactionRepository = InMemoryTransactionRepository()
        accountService = AccountService(
            accountRepository, transactionRepository, InMemoryImportBatchRepository())
    }

    @Test
    fun `creates account with name and type`() {
        val account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK)

        assertThat(account.name()).isEqualTo("CommBank Everyday")
        assertThat(account.type()).isEqualTo(AccountType.COMMBANK)
        assertThat(account.hasImports()).isFalse()
        assertThat(account.id()).isNotNull()
    }

    @Test
    fun `deletes existing account`() {
        val account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK)

        accountService.deleteAccount(account.id())

        assertThatThrownBy { accountService.getAccount(account.id()) }
            .isInstanceOf(AccountNotFoundException::class.java)
    }

    @Test
    fun `deletes transactions when account is deleted`() {
        val account = accountService.createAccount("CommBank Everyday", AccountType.COMMBANK)
        val uncategorised = Category.uncategorised()
        val transaction = Transaction.create(
            account.id(),
            ParsedTransactionRow(
                LocalDate.of(2026, 7, 10),
                BigDecimal("-45.00"),
                "WOOLWORTHS",
                BigDecimal("2455.00")),
            uncategorised.id)
        transactionRepository.save(transaction)

        accountService.deleteAccount(account.id())

        assertThat(transactionRepository.existsByAccountAndFingerprint(account.id(), transaction.fingerprint()))
            .isFalse()
    }

    @Test
    fun `rejects blank account name`() {
        assertThatThrownBy { accountService.createAccount("  ", AccountType.COMMBANK) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("name")
    }

    @Test
    fun `lists created accounts`() {
        val first = accountService.createAccount("Everyday", AccountType.COMMBANK)
        val second = accountService.createAccount("Savings", AccountType.ING)

        assertThat(accountService.listAccounts()).containsExactlyInAnyOrder(first, second)
    }

    @Test
    fun `ensure fixed accounts creates one per type`() {
        val accounts = accountService.ensureFixedAccounts()

        assertThat(accounts).hasSize(AccountType.entries.size)
        assertThat(accounts).extracting<AccountType> { it.type() }.containsExactly(*AccountType.entries.toTypedArray())
        assertThat(accounts).extracting<String> { it.name() }
            .containsExactly("CommBank", "ING", "NAB credit card")

        val again = accountService.ensureFixedAccounts()
        assertThat(again).extracting<AccountId> { it.id() }.containsExactlyElementsOf(
            accounts.map { it.id() })
    }
}