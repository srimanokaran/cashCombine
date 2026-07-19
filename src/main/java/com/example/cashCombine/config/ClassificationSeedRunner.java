package com.example.cashCombine.config;

import com.example.cashCombine.ledger.categorisation.Category;
import com.example.cashCombine.ledger.categorisation.CategoryRepository;
import com.example.cashCombine.ledger.categorisation.ClassificationRule;
import com.example.cashCombine.ledger.categorisation.ClassificationRuleRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds starter categories and contains-match rules (idempotent).
 * Existing seed patterns are updated if their target category changes.
 */
@Component
@Order(1)
public class ClassificationSeedRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger("api");

	private static final List<String> CATEGORIES = List.of(
			"Groceries",
			"Dining",
			"Transport",
			"Fuel",
			"Streaming",
			"Shopping",
			"Utilities",
			"Health",
			"Home",
			"Credit cards",
			"Rent",
			Category.FUNDS_BETWEEN_ACCOUNTS_NAME,
			"Income");

	/**
	 * Broad / broken patterns previously seeded.
	 * - transfer to/from: too blunt
	 * - "BP": was "BP " but trim() stripped the space, so it matched BPAY credit-card bills
	 */
	private static final Set<String> OBSOLETE_PATTERNS = Set.of("transfer to", "transfer from", "bp");

	/** pattern → category name */
	private static final Map<String, String> RULES = seedRules();

	private final CategoryRepository categoryRepository;
	private final ClassificationRuleRepository ruleRepository;

	public ClassificationSeedRunner(
			CategoryRepository categoryRepository, ClassificationRuleRepository ruleRepository) {
		this.categoryRepository = categoryRepository;
		this.ruleRepository = ruleRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		Map<String, Category> categoriesByName = new LinkedHashMap<>();
		for (String name : CATEGORIES) {
			Category category = categoryRepository
					.findByName(name)
					.orElseGet(() -> categoryRepository.save(Category.create(name)));
			categoriesByName.put(name.toLowerCase(Locale.ROOT), category);
		}

		int removed = removeObsoleteRules();

		Map<String, ClassificationRule> existingByPattern = ruleRepository.findAll().stream()
				.collect(Collectors.toMap(
						rule -> rule.pattern().toLowerCase(Locale.ROOT),
						rule -> rule,
						(first, ignored) -> first,
						LinkedHashMap::new));

		int created = 0;
		int updated = 0;
		for (Map.Entry<String, String> entry : RULES.entrySet()) {
			String pattern = entry.getKey();
			Category category = categoriesByName.get(entry.getValue().toLowerCase(Locale.ROOT));
			if (category == null) {
				continue;
			}

			ClassificationRule existing = existingByPattern.get(pattern.toLowerCase(Locale.ROOT));
			if (existing != null) {
				if (existing.categoryId().equals(category.id())) {
					continue;
				}
				ruleRepository.deleteById(existing.id());
				ruleRepository.save(ClassificationRule.create(pattern, category.id()));
				updated++;
				continue;
			}

			ruleRepository.save(ClassificationRule.create(pattern, category.id()));
			created++;
		}

		if (created > 0 || updated > 0 || removed > 0) {
			log.info(
					"Classification seed: created {}, updated {}, removed {} obsolete ({} categories ensured)",
					created,
					updated,
					removed,
					CATEGORIES.size());
		}
	}

	private int removeObsoleteRules() {
		int removed = 0;
		for (ClassificationRule rule : ruleRepository.findAll()) {
			if (OBSOLETE_PATTERNS.contains(rule.pattern().toLowerCase(Locale.ROOT))) {
				ruleRepository.deleteById(rule.id());
				removed++;
			}
		}
		return removed;
	}

	private static Map<String, String> seedRules() {
		Map<String, String> rules = new LinkedHashMap<>();
		rules.put("WOOLWORTHS", "Groceries");
		rules.put("COLES", "Groceries");
		rules.put("ALDI", "Groceries");
		rules.put("IGA", "Groceries");
		rules.put("CAFE", "Dining");
		rules.put("COFFEE", "Dining");
		rules.put("MCDONALD", "Dining");
		rules.put("GUZMAN", "Dining");
		rules.put("DOORDASH", "Dining");
		rules.put("UBER EATS", "Dining");
		rules.put("MENULOG", "Dining");
		rules.put("UBER", "Dining");
		rules.put("OLA", "Transport");
		rules.put("MYKI", "Transport");
		rules.put("TRANSPORT", "Transport");
		rules.put("SHELL", "Fuel");
		rules.put("BP ", "Fuel");
		rules.put("CALTEX", "Fuel");
		rules.put("7-ELEVEN", "Fuel");
		rules.put("NETFLIX", "Streaming");
		rules.put("SPOTIFY", "Streaming");
		rules.put("DISNEY", "Streaming");
		rules.put("HBOMAX", "Streaming");
		rules.put("STREAMING", "Streaming");
		rules.put("AMAZON", "Shopping");
		rules.put("KMART", "Shopping");
		rules.put("TARGET", "Shopping");
		rules.put("JB HI", "Shopping");
		rules.put("OPTUS", "Utilities");
		rules.put("TELSTRA", "Utilities");
		rules.put("VODAFONE", "Utilities");
		rules.put("AGL", "Utilities");
		rules.put("ORIGIN ENERGY", "Utilities");
		rules.put("CHEMIST", "Health");
		rules.put("PHARMACY", "Health");
		rules.put("Top Gym", "Health");
		rules.put("Laundrette", "Home");
		rules.put("Qantas Credit Cards", "Credit cards");
		rules.put("Transfer To Landlord", "Rent");
		rules.put("CommBank App Savings", Category.FUNDS_BETWEEN_ACCOUNTS_NAME);
		rules.put("PAYROLL", "Income");
		rules.put("Direct Credit", "Income");
		return Map.copyOf(rules);
	}

}
