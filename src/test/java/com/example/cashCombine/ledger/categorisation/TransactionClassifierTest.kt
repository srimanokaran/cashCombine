package com.example.cashCombine.ledger.categorisation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TransactionClassifierTest {

    private lateinit var uncategorised: Category
    private lateinit var groceries: Category
    private lateinit var streaming: Category
    private lateinit var ruleRepository: ClassificationRuleRepository
    private lateinit var classifier: TransactionClassifier

    @BeforeEach
    fun setUp() {
        val categoryRepository = InMemoryCategoryRepository()
        uncategorised = categoryRepository.save(Category.uncategorised())
        groceries = categoryRepository.save(Category.create("Groceries"))
        streaming = categoryRepository.save(Category.create("Streaming"))

        ruleRepository = InMemoryClassificationRuleRepository()
        classifier = TransactionClassifier(ruleRepository, uncategorised.id)
    }

    @Test
    fun `assigns matching rule category`() {
        ruleRepository.save(ClassificationRule.create("WOOLWORTHS", groceries.id))

        assertThat(classifier.classify("WOOLWORTHS 1234 FAKETOWN")).isEqualTo(groceries.id)
    }

    @Test
    fun `matching is case-insensitive`() {
        ruleRepository.save(ClassificationRule.create("netflix", streaming.id))

        assertThat(classifier.classify("Netflix.com Melbourne")).isEqualTo(streaming.id)
    }

    @Test
    fun `falls back to Uncategorised when no rule matches`() {
        ruleRepository.save(ClassificationRule.create("WOOLWORTHS", groceries.id))

        assertThat(classifier.classify("RANDOM MERCHANT XYZ")).isEqualTo(uncategorised.id)
    }

    @Test
    fun `prefers longer more specific pattern over broader seed rule`() {
        val transport = Category.create("Transport")
        ruleRepository.save(ClassificationRule.create("UBER", transport.id))
        ruleRepository.save(ClassificationRule.create("UBER *ONE", streaming.id))

        assertThat(classifier.classify("UBER *ONE MEMBERSHIP")).isEqualTo(streaming.id)
    }

    @Test
    fun `preserves trailing space so BP fuel does not match BPAY`() {
        val fuel = Category.create("Fuel")
        ruleRepository.save(ClassificationRule.create("BP ", fuel.id))

        assertThat(classifier.classify("BP EXPRESS HIGHWAY")).isEqualTo(fuel.id)
        assertThat(
            classifier.classify(
                "Qantas Credit Cards CommBank app BPAY 000000 0000000000000000 Bill"
            )
        ).isEqualTo(uncategorised.id)
    }

    @Test
    fun `classifies ING direct credit as Funds between accounts not Income`() {
        val funds = Category.create(Category.FUNDS_BETWEEN_ACCOUNTS_NAME)
        val income = Category.create(Category.INCOME_NAME)
        ruleRepository.save(ClassificationRule.create(" ING ", funds.id))
        ruleRepository.save(ClassificationRule.create("PAYROLL", income.id))

        assertThat(classifier.classify("Direct Credit 000000 ING 000000000 0000000"))
            .isEqualTo(funds.id)
        assertThat(classifier.classify("KMART SHOPPING CENTRE")).isEqualTo(uncategorised.id)
        assertThat(classifier.classify("Direct Credit ACME PAYROLL")).isEqualTo(income.id)
        assertThat(classifier.classify("Direct Credit ACME REFUND")).isEqualTo(uncategorised.id)
    }

    @Test
    fun `classifies Qantas credit card BPAY as Funds between accounts`() {
        val funds = Category.create(Category.FUNDS_BETWEEN_ACCOUNTS_NAME)
        ruleRepository.save(ClassificationRule.create("Qantas Credit Cards", funds.id))

        assertThat(
            classifier.classify(
                "Qantas Credit Cards CommBank app BPAY 000000 0000000000000000 Bill"
            )
        ).isEqualTo(funds.id)
    }
}