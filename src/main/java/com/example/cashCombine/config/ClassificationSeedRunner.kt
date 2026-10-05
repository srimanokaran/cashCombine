package com.example.cashCombine.config

import com.example.cashCombine.ledger.categorisation.Category
import com.example.cashCombine.ledger.categorisation.CategoryRepository
import com.example.cashCombine.ledger.categorisation.ClassificationRule
import com.example.cashCombine.ledger.categorisation.ClassificationRuleRepository
import com.example.cashCombine.ledger.categorisation.RulePattern
import com.example.cashCombine.ledger.transactions.TransactionRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.Locale

@Component
@Order(1)
open class ClassificationSeedRunner(
    private val categoryRepository: CategoryRepository,
    private val ruleRepository: ClassificationRuleRepository,
    private val transactionRepository: TransactionRepository,
) : ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments) {
        renameStreamingToSubscription()
        mergeHomeIntoUtilities()
        mergeCreditCardsIntoFundsBetweenAccounts()
        stabilizeNoisyRulePatterns()

        val categoriesByName = LinkedHashMap<String, Category>()
        for (name in CATEGORIES) {
            val category = categoryRepository.findByName(name)
                ?: categoryRepository.save(Category.create(name))
            categoriesByName[name.lowercase(Locale.ROOT)] = category
        }

        val removed = removeObsoleteRules()

        val existingByPattern = ruleRepository.findAll().associateByTo(LinkedHashMap()) {
            it.pattern.lowercase(Locale.ROOT)
        }

        var created = 0
        var updated = 0
        for ((pattern, value) in RULES) {
            val category = categoriesByName[value.lowercase(Locale.ROOT)] ?: continue

            val existing = existingByPattern[pattern.lowercase(Locale.ROOT)]
            if (existing != null) {
                if (existing.categoryId == category.id) {
                    continue
                }
                ruleRepository.deleteById(existing.id)
                ruleRepository.save(ClassificationRule.create(pattern, category.id))
                updated++
                continue
            }

            ruleRepository.save(ClassificationRule.create(pattern, category.id))
            created++
        }

        if (created > 0 || updated > 0 || removed > 0) {
            log.info(
                "Classification seed: created {}, updated {}, removed {} obsolete ({} categories ensured)",
                created,
                updated,
                removed,
                CATEGORIES.size)
        }
    }

    private fun stabilizeNoisyRulePatterns() {
        val seenStable = ruleRepository.findAll()
            .map { it.pattern.lowercase(Locale.ROOT) }
            .toMutableSet()

        var shortened = 0
        var dropped = 0
        for (rule in ruleRepository.findAll().toList()) {
            val stable = RulePattern.fromDescription(rule.pattern)
            if (stable == rule.pattern) {
                continue
            }
            ruleRepository.deleteById(rule.id)
            val key = stable.lowercase(Locale.ROOT)
            if (seenStable.contains(key)) {
                dropped++
                continue
            }
            ruleRepository.save(ClassificationRule.create(stable, rule.categoryId))
            seenStable.add(key)
            shortened++
        }

        if (shortened > 0 || dropped > 0) {
            log.info(
                "Stabilised noisy classification rules: shortened {}, dropped {} duplicates",
                shortened,
                dropped)
        }
    }

    private fun renameStreamingToSubscription() {
        val streaming = categoryRepository.findByName("Streaming") ?: return
        if (categoryRepository.findByName("Subscription") != null) {
            return
        }
        categoryRepository.save(Category.reconstitute(streaming.id, "Subscription"))
        log.info("Renamed category Streaming → Subscription")
    }

    private fun mergeHomeIntoUtilities() {
        val home = categoryRepository.findByName("Home") ?: return

        val utilities = categoryRepository.findByName("Utilities")
        if (utilities == null) {
            categoryRepository.save(Category.reconstitute(home.id, "Utilities"))
            log.info("Renamed category Home → Utilities")
            return
        }

        var movedTransactions = 0
        for (transaction in transactionRepository.findByCategoryId(home.id)) {
            transaction.reassignCategory(utilities.id)
            transactionRepository.save(transaction)
            movedTransactions++
        }

        val utilityPatterns = ruleRepository.findAll()
            .filter { it.categoryId == utilities.id }
            .map { it.pattern.lowercase(Locale.ROOT) }
            .toSet()

        var movedRules = 0
        for (rule in ruleRepository.findAll().toList()) {
            if (rule.categoryId != home.id) {
                continue
            }
            ruleRepository.deleteById(rule.id)
            if (!utilityPatterns.contains(rule.pattern.lowercase(Locale.ROOT))) {
                ruleRepository.save(ClassificationRule.create(rule.pattern, utilities.id))
                movedRules++
            }
        }

        categoryRepository.deleteById(home.id)
        log.info(
            "Merged Home into Utilities ({} transactions, {} rules)",
            movedTransactions,
            movedRules)
    }

    private fun mergeCreditCardsIntoFundsBetweenAccounts() {
        val creditCards = categoryRepository.findByName("Credit cards") ?: return

        val funds = categoryRepository.findByName(Category.FUNDS_BETWEEN_ACCOUNTS_NAME)
            ?: categoryRepository.save(Category.create(Category.FUNDS_BETWEEN_ACCOUNTS_NAME))

        var movedTransactions = 0
        for (transaction in transactionRepository.findByCategoryId(creditCards.id)) {
            transaction.reassignCategory(funds.id)
            transactionRepository.save(transaction)
            movedTransactions++
        }

        val fundsPatterns = ruleRepository.findAll()
            .filter { it.categoryId == funds.id }
            .map { it.pattern.lowercase(Locale.ROOT) }
            .toSet()

        var movedRules = 0
        for (rule in ruleRepository.findAll().toList()) {
            if (rule.categoryId != creditCards.id) {
                continue
            }
            ruleRepository.deleteById(rule.id)
            if (!fundsPatterns.contains(rule.pattern.lowercase(Locale.ROOT))) {
                ruleRepository.save(ClassificationRule.create(rule.pattern, funds.id))
                movedRules++
            }
        }

        categoryRepository.deleteById(creditCards.id)
        log.info(
            "Merged Credit cards into Funds between accounts ({} transactions, {} rules)",
            movedTransactions,
            movedRules)
    }

    private fun removeObsoleteRules(): Int {
        var removed = 0
        for (rule in ruleRepository.findAll()) {
            if (OBSOLETE_PATTERNS.contains(rule.pattern.lowercase(Locale.ROOT))) {
                ruleRepository.deleteById(rule.id)
                removed++
            }
        }
        return removed
    }

    companion object {
        private val log = LoggerFactory.getLogger("api")

        private val CATEGORIES = listOf(
            "Groceries",
            "Dining",
            "Transport",
            "Fuel",
            "Subscription",
            "Shopping",
            "Utilities",
            "Health",
            "Rent",
            Category.FUNDS_BETWEEN_ACCOUNTS_NAME,
            Category.INCOME_NAME)

        private val OBSOLETE_PATTERNS =
            setOf("transfer to", "transfer from", "bp", "direct credit")

        private val RULES = seedRules()

        private fun seedRules(): Map<String, String> {
            val rules = LinkedHashMap<String, String>()
            rules["WOOLWORTHS"] = "Groceries"
            rules["COLES"] = "Groceries"
            rules["ALDI"] = "Groceries"
            rules["IGA"] = "Groceries"
            rules["CAFE"] = "Dining"
            rules["COFFEE"] = "Dining"
            rules["MCDONALD"] = "Dining"
            rules["GUZMAN"] = "Dining"
            rules["DOORDASH"] = "Dining"
            rules["UBER EATS"] = "Dining"
            rules["MENULOG"] = "Dining"
            rules["UBER"] = "Dining"
            rules["OLA"] = "Transport"
            rules["MYKI"] = "Transport"
            rules["TRANSPORT"] = "Transport"
            rules["SHELL"] = "Fuel"
            rules["BP "] = "Fuel"
            rules["CALTEX"] = "Fuel"
            rules["7-ELEVEN"] = "Fuel"
            rules["NETFLIX"] = "Subscription"
            rules["SPOTIFY"] = "Subscription"
            rules["DISNEY"] = "Subscription"
            rules["HBOMAX"] = "Subscription"
            rules["STREAMING"] = "Subscription"
            rules["AMAZON"] = "Shopping"
            rules["KMART"] = "Shopping"
            rules["TARGET"] = "Shopping"
            rules["JB HI"] = "Shopping"
            rules["OPTUS"] = "Utilities"
            rules["TELSTRA"] = "Utilities"
            rules["VODAFONE"] = "Utilities"
            rules["AGL"] = "Utilities"
            rules["ORIGIN ENERGY"] = "Utilities"
            rules["Laundrette"] = "Utilities"
            rules["CHEMIST"] = "Health"
            rules["PHARMACY"] = "Health"
            rules["Top Gym"] = "Health"
            rules["Qantas Credit Cards"] = Category.FUNDS_BETWEEN_ACCOUNTS_NAME
            rules["Transfer To Landlord"] = "Rent"
            rules["CommBank App Savings"] = Category.FUNDS_BETWEEN_ACCOUNTS_NAME
            rules[" ING "] = Category.FUNDS_BETWEEN_ACCOUNTS_NAME
            rules["BPAY PAYMENT"] = Category.FUNDS_BETWEEN_ACCOUNTS_NAME
            rules["PAYROLL"] = Category.INCOME_NAME
            return rules
        }
    }
}