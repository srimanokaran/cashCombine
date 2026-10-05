package com.example.cashCombine.ledger.categorisation

class ClassificationRuleService(
    private val ruleRepository: ClassificationRuleRepository,
    private val categoryRepository: CategoryRepository
) {

    fun createRule(pattern: String, categoryId: CategoryId): ClassificationRule {
        if (categoryRepository.findById(categoryId) == null) {
            throw CategoryNotFoundException(categoryId)
        }
        val rule = ClassificationRule.create(pattern, categoryId)
        return ruleRepository.save(rule)
    }

    /**
     * Creates a rule for the pattern, or retargets an existing same-pattern rule (case-insensitive).
     */
    fun upsertRule(pattern: String?, categoryId: CategoryId): ClassificationRule {
        if (categoryRepository.findById(categoryId) == null) {
            throw CategoryNotFoundException(categoryId)
        }
        val normalised = pattern ?: ""
        val existing = ruleRepository.findAll()
            .filter { it.pattern.equals(normalised, ignoreCase = true) }
            .firstOrNull()
        if (existing != null) {
            if (existing.categoryId == categoryId) {
                return existing
            }
            ruleRepository.deleteById(existing.id)
        }
        return ruleRepository.save(ClassificationRule.create(normalised, categoryId))
    }

    fun listRules(): List<ClassificationRule> = ruleRepository.findAll()

    fun deleteRule(ruleId: ClassificationRuleId) {
        if (ruleRepository.findById(ruleId) == null) {
            throw RuleNotFoundException(ruleId)
        }
        ruleRepository.deleteById(ruleId)
    }
}