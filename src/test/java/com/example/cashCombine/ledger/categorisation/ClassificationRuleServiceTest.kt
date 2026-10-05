package com.example.cashCombine.ledger.categorisation

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ClassificationRuleServiceTest {

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var ruleService: ClassificationRuleService
    private lateinit var groceries: Category

    @BeforeEach
    fun setUp() {
        categoryRepository = InMemoryCategoryRepository()
        groceries = categoryRepository.save(Category.create("Groceries"))
        ruleService = ClassificationRuleService(InMemoryClassificationRuleRepository(), categoryRepository)
    }

    @Test
    fun `creates and lists rules`() {
        val rule = ruleService.createRule("WOOLWORTHS", groceries.id)

        assertThat(rule.pattern).isEqualTo("WOOLWORTHS")
        assertThat(rule.categoryId).isEqualTo(groceries.id)
        assertThat(ruleService.listRules()).containsExactly(rule)
    }

    @Test
    fun `rejects unknown category`() {
        assertThatThrownBy { ruleService.createRule("WOOLWORTHS", CategoryId.generate()) }
            .isInstanceOf(CategoryNotFoundException::class.java)
    }

    @Test
    fun `rejects blank pattern`() {
        assertThatThrownBy { ruleService.createRule("  ", groceries.id) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("pattern")
    }

    @Test
    fun `deletes existing rule`() {
        val rule = ruleService.createRule("WOOLWORTHS", groceries.id)

        ruleService.deleteRule(rule.id)

        assertThat(ruleService.listRules()).isEmpty()
    }

    @Test
    fun `delete rejects unknown rule`() {
        assertThatThrownBy { ruleService.deleteRule(ClassificationRuleId.generate()) }
            .isInstanceOf(RuleNotFoundException::class.java)
    }
}