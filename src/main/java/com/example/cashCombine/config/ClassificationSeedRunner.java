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
			"Transfers",
			"Income");

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

		Set<String> existingPatterns = ruleRepository.findAll().stream()
				.map(rule -> rule.pattern().toLowerCase(Locale.ROOT))
				.collect(Collectors.toSet());

		int created = 0;
		for (Map.Entry<String, String> entry : RULES.entrySet()) {
			String pattern = entry.getKey();
			if (existingPatterns.contains(pattern.toLowerCase(Locale.ROOT))) {
				continue;
			}
			Category category = categoriesByName.get(entry.getValue().toLowerCase(Locale.ROOT));
			if (category == null) {
				continue;
			}
			ruleRepository.save(ClassificationRule.create(pattern, category.id()));
			created++;
		}

		if (created > 0) {
			log.info("Seeded {} classification rules ({} categories ensured)", created, CATEGORIES.size());
		}
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
		rules.put("UBER EATS", "Dining");
		rules.put("MENULOG", "Dining");
		rules.put("UBER", "Transport");
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
		rules.put("STREAMING", "Streaming");
		rules.put("AMAZON", "Shopping");
		rules.put("KMART", "Shopping");
		rules.put("TARGET", "Shopping");
		rules.put("JB HI", "Shopping");
		rules.put("OPTUS", "Utilities");
		rules.put("TELSTRA", "Utilities");
		rules.put("AGL", "Utilities");
		rules.put("ORIGIN ENERGY", "Utilities");
		rules.put("CHEMIST", "Health");
		rules.put("PHARMACY", "Health");
		rules.put("Transfer To", "Transfers");
		rules.put("Transfer From", "Transfers");
		rules.put("PAYROLL", "Income");
		rules.put("Direct Credit", "Income");
		return Map.copyOf(rules);
	}

}
