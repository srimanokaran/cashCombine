package com.example.cashCombine.ledger.categorisation

class InMemoryClassificationRuleRepository : ClassificationRuleRepository {

    private val rules = LinkedHashMap<ClassificationRuleId, ClassificationRule>()

    override fun save(rule: ClassificationRule): ClassificationRule {
        rules[rule.id] = rule
        return rule
    }

    override fun findById(id: ClassificationRuleId): ClassificationRule? = rules[id]

    override fun findAll(): List<ClassificationRule> = ArrayList(rules.values)

    override fun deleteById(id: ClassificationRuleId) {
        rules.remove(id)
    }

    override fun deleteByCategoryId(categoryId: CategoryId) {
        rules.entries.removeIf { it.value.categoryId == categoryId }
    }
}