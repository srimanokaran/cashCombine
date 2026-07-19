package com.example.cashCombine.ledger.transactions;

import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.categorisation.CategoryId;
import com.example.cashCombine.ledger.categorisation.CategoryNotFoundException;
import com.example.cashCombine.ledger.categorisation.CategoryRepository;
import com.example.cashCombine.ledger.categorisation.ClassificationRule;
import com.example.cashCombine.ledger.categorisation.ClassificationRuleService;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public class TransactionService {

	private final TransactionRepository transactionRepository;
	private final CategoryRepository categoryRepository;
	private final ClassificationRuleService classificationRuleService;

	public TransactionService(
			TransactionRepository transactionRepository,
			CategoryRepository categoryRepository,
			ClassificationRuleService classificationRuleService) {
		this.transactionRepository = transactionRepository;
		this.categoryRepository = categoryRepository;
		this.classificationRuleService = classificationRuleService;
	}

	/**
	 * Manually categorises a transaction, upserts a contains-match rule from its full description,
	 * and applies that rule to other non-manual matching transactions.
	 */
	@Transactional
	public Transaction changeCategory(TransactionId transactionId, CategoryId categoryId) {
		Transaction transaction = transactionRepository.findById(transactionId)
				.orElseThrow(() -> new TransactionNotFoundException(transactionId));
		if (categoryRepository.findById(categoryId).isEmpty()) {
			throw new CategoryNotFoundException(categoryId);
		}

		transaction.changeCategory(categoryId);
		Transaction saved = transactionRepository.save(transaction);

		ClassificationRule rule =
				classificationRuleService.upsertRule(saved.description(), categoryId);
		applyRuleToMatchingTransactions(rule, saved.id());

		return saved;
	}

	private void applyRuleToMatchingTransactions(ClassificationRule rule, TransactionId skipId) {
		for (Transaction other : transactionRepository.findAll()) {
			if (other.id().equals(skipId)) {
				continue;
			}
			if (other.isManuallyCategorised()) {
				continue;
			}
			if (!rule.matches(other.description())) {
				continue;
			}
			if (other.categoryId().equals(rule.categoryId())) {
				continue;
			}
			other.applyRuleCategory(rule.categoryId());
			transactionRepository.save(other);
		}
	}

	public Transaction getTransaction(TransactionId transactionId) {
		return transactionRepository.findById(transactionId)
				.orElseThrow(() -> new TransactionNotFoundException(transactionId));
	}

	public List<Transaction> listByAccount(AccountId accountId) {
		return transactionRepository.findByAccountId(accountId);
	}

}
