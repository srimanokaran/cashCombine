package com.example.cashCombine.ledger.transactions;

import com.example.cashCombine.ledger.categorisation.CategoryId;
import com.example.cashCombine.ledger.categorisation.CategoryNotFoundException;
import com.example.cashCombine.ledger.categorisation.CategoryRepository;

public class TransactionService {

	private final TransactionRepository transactionRepository;
	private final CategoryRepository categoryRepository;

	public TransactionService(TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
		this.transactionRepository = transactionRepository;
		this.categoryRepository = categoryRepository;
	}

	public Transaction changeCategory(TransactionId transactionId, CategoryId categoryId) {
		Transaction transaction = transactionRepository.findById(transactionId)
				.orElseThrow(() -> new TransactionNotFoundException(transactionId));
		if (categoryRepository.findById(categoryId).isEmpty()) {
			throw new CategoryNotFoundException(categoryId);
		}

		transaction.changeCategory(categoryId);
		return transactionRepository.save(transaction);
	}

	public Transaction getTransaction(TransactionId transactionId) {
		return transactionRepository.findById(transactionId)
				.orElseThrow(() -> new TransactionNotFoundException(transactionId));
	}

}
