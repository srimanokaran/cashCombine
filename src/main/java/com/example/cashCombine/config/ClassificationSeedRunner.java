package com.example.cashCombine.config;

import com.example.cashCombine.ledger.categorisation.Category;
import com.example.cashCombine.ledger.categorisation.CategoryRepository;
import com.example.cashCombine.ledger.categorisation.ClassificationRule;
import com.example.cashCombine.ledger.categorisation.ClassificationRuleRepository;
import com.example.cashCombine.ledger.categorisation.RulePattern;
import com.example.cashCombine.ledger.transactions.Transaction;
import com.example.cashCombine.ledger.transactions.TransactionRepository;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
			"Subscription",
			"Shopping",
			"Utilities",
			"Health",
			"Rent",
			Category.FUNDS_BETWEEN_ACCOUNTS_NAME,
			Category.INCOME_NAME);

	/**
	 * Broad / broken patterns previously seeded.
	 * - transfer to/from: too blunt
	 * - "BP": was "BP " but trim() stripped the space, so it matched BPAY credit-card bills
	 * - "direct credit": caught ING→CommBank transfers as income
	 */
	private static final Set<String> OBSOLETE_PATTERNS =
			Set.of("transfer to", "transfer from", "bp", "direct credit");

	/** pattern → category name */
	private static final Map<String, String> RULES = seedRules();

	private final CategoryRepository categoryRepository;
	private final ClassificationRuleRepository ruleRepository;
	private final TransactionRepository transactionRepository;

	public ClassificationSeedRunner(
			CategoryRepository categoryRepository,
			ClassificationRuleRepository ruleRepository,
			TransactionRepository transactionRepository) {
		this.categoryRepository = categoryRepository;
		this.ruleRepository = ruleRepository;
		this.transactionRepository = transactionRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		renameStreamingToSubscription();
		mergeHomeIntoUtilities();
		mergeCreditCardsIntoFundsBetweenAccounts();
		stabilizeNoisyRulePatterns();

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

	/**
	 * Manual overrides used to save the full CommBank description (including Card xx / Value Date).
	 * Rewrite those to a stable merchant prefix and drop duplicates.
	 */
	private void stabilizeNoisyRulePatterns() {
		Set<String> seenStable = ruleRepository.findAll().stream()
				.map(rule -> rule.pattern().toLowerCase(Locale.ROOT))
				.collect(Collectors.toCollection(HashSet::new));

		int shortened = 0;
		int dropped = 0;
		for (ClassificationRule rule : List.copyOf(ruleRepository.findAll())) {
			String stable = RulePattern.fromDescription(rule.pattern());
			if (stable.equals(rule.pattern())) {
				continue;
			}
			ruleRepository.deleteById(rule.id());
			String key = stable.toLowerCase(Locale.ROOT);
			if (seenStable.contains(key)) {
				dropped++;
				continue;
			}
			ruleRepository.save(ClassificationRule.create(stable, rule.categoryId()));
			seenStable.add(key);
			shortened++;
		}

		if (shortened > 0 || dropped > 0) {
			log.info(
					"Stabilised noisy classification rules: shortened {}, dropped {} duplicates",
					shortened,
					dropped);
		}
	}

	private void renameStreamingToSubscription() {
		var streaming = categoryRepository.findByName("Streaming");
		if (streaming.isEmpty() || categoryRepository.findByName("Subscription").isPresent()) {
			return;
		}
		categoryRepository.save(Category.reconstitute(streaming.get().id(), "Subscription"));
		log.info("Renamed category Streaming → Subscription");
	}

	private void mergeHomeIntoUtilities() {
		Optional<Category> home = categoryRepository.findByName("Home");
		if (home.isEmpty()) {
			return;
		}

		Optional<Category> utilities = categoryRepository.findByName("Utilities");
		if (utilities.isEmpty()) {
			categoryRepository.save(Category.reconstitute(home.get().id(), "Utilities"));
			log.info("Renamed category Home → Utilities");
			return;
		}

		Category homeCategory = home.get();
		Category utilitiesCategory = utilities.get();
		int movedTransactions = 0;
		for (Transaction transaction : transactionRepository.findByCategoryId(homeCategory.id())) {
			transaction.reassignCategory(utilitiesCategory.id());
			transactionRepository.save(transaction);
			movedTransactions++;
		}

		Set<String> utilityPatterns = ruleRepository.findAll().stream()
				.filter(rule -> rule.categoryId().equals(utilitiesCategory.id()))
				.map(rule -> rule.pattern().toLowerCase(Locale.ROOT))
				.collect(Collectors.toSet());

		int movedRules = 0;
		for (ClassificationRule rule : List.copyOf(ruleRepository.findAll())) {
			if (!rule.categoryId().equals(homeCategory.id())) {
				continue;
			}
			ruleRepository.deleteById(rule.id());
			if (!utilityPatterns.contains(rule.pattern().toLowerCase(Locale.ROOT))) {
				ruleRepository.save(ClassificationRule.create(rule.pattern(), utilitiesCategory.id()));
				movedRules++;
			}
		}

		categoryRepository.deleteById(homeCategory.id());
		log.info(
				"Merged Home into Utilities ({} transactions, {} rules)",
				movedTransactions,
				movedRules);
	}

	/**
	 * Card payments from cash accounts are transfers, not spend. Merchant detail lives on the
	 * credit-card account and counts toward Expenses by category.
	 */
	private void mergeCreditCardsIntoFundsBetweenAccounts() {
		Optional<Category> creditCards = categoryRepository.findByName("Credit cards");
		if (creditCards.isEmpty()) {
			return;
		}

		Category funds = categoryRepository
				.findByName(Category.FUNDS_BETWEEN_ACCOUNTS_NAME)
				.orElseGet(() -> categoryRepository.save(Category.create(Category.FUNDS_BETWEEN_ACCOUNTS_NAME)));

		Category creditCardsCategory = creditCards.get();
		int movedTransactions = 0;
		for (Transaction transaction : transactionRepository.findByCategoryId(creditCardsCategory.id())) {
			transaction.reassignCategory(funds.id());
			transactionRepository.save(transaction);
			movedTransactions++;
		}

		Set<String> fundsPatterns = ruleRepository.findAll().stream()
				.filter(rule -> rule.categoryId().equals(funds.id()))
				.map(rule -> rule.pattern().toLowerCase(Locale.ROOT))
				.collect(Collectors.toSet());

		int movedRules = 0;
		for (ClassificationRule rule : List.copyOf(ruleRepository.findAll())) {
			if (!rule.categoryId().equals(creditCardsCategory.id())) {
				continue;
			}
			ruleRepository.deleteById(rule.id());
			if (!fundsPatterns.contains(rule.pattern().toLowerCase(Locale.ROOT))) {
				ruleRepository.save(ClassificationRule.create(rule.pattern(), funds.id()));
				movedRules++;
			}
		}

		categoryRepository.deleteById(creditCardsCategory.id());
		log.info(
				"Merged Credit cards into Funds between accounts ({} transactions, {} rules)",
				movedTransactions,
				movedRules);
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
		rules.put("NETFLIX", "Subscription");
		rules.put("SPOTIFY", "Subscription");
		rules.put("DISNEY", "Subscription");
		rules.put("HBOMAX", "Subscription");
		rules.put("STREAMING", "Subscription");
		rules.put("AMAZON", "Shopping");
		rules.put("KMART", "Shopping");
		rules.put("TARGET", "Shopping");
		rules.put("JB HI", "Shopping");
		rules.put("OPTUS", "Utilities");
		rules.put("TELSTRA", "Utilities");
		rules.put("VODAFONE", "Utilities");
		rules.put("AGL", "Utilities");
		rules.put("ORIGIN ENERGY", "Utilities");
		rules.put("Laundrette", "Utilities");
		rules.put("CHEMIST", "Health");
		rules.put("PHARMACY", "Health");
		rules.put("Top Gym", "Health");
		rules.put("Qantas Credit Cards", Category.FUNDS_BETWEEN_ACCOUNTS_NAME);
		rules.put("Transfer To Landlord", "Rent");
		rules.put("CommBank App Savings", Category.FUNDS_BETWEEN_ACCOUNTS_NAME);
		// Spaces avoid matching substrings like SHOPPING.
		rules.put(" ING ", Category.FUNDS_BETWEEN_ACCOUNTS_NAME);
		rules.put("BPAY PAYMENT", Category.FUNDS_BETWEEN_ACCOUNTS_NAME);
		rules.put("PAYROLL", Category.INCOME_NAME);
		// Preserve LinkedHashMap encounter order (Map.copyOf does not).
		return Collections.unmodifiableMap(rules);
	}

}
