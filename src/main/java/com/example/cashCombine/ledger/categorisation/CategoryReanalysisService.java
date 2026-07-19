package com.example.cashCombine.ledger.categorisation;

import com.example.cashCombine.ledger.transactions.Transaction;
import com.example.cashCombine.ledger.transactions.TransactionRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Re-runs contains-match rules against existing transactions.
 * Manual category overrides are left unchanged.
 */
public class CategoryReanalysisService {

	private final TransactionRepository transactionRepository;
	private final TransactionClassifier classifier;

	public CategoryReanalysisService(
			TransactionRepository transactionRepository, TransactionClassifier classifier) {
		this.transactionRepository = transactionRepository;
		this.classifier = classifier;
	}

	@Transactional
	public CategoryReanalysisResult reanalyse() {
		int examined = 0;
		int updated = 0;
		int skippedManual = 0;

		for (Transaction transaction : transactionRepository.findAll()) {
			examined++;
			if (transaction.isManuallyCategorised()) {
				skippedManual++;
				continue;
			}

			CategoryId classified = classifier.classify(transaction.description());
			if (classified.equals(transaction.categoryId())) {
				continue;
			}

			transaction.applyRuleCategory(classified);
			transactionRepository.save(transaction);
			updated++;
		}

		return new CategoryReanalysisResult(examined, updated, skippedManual);
	}

}
