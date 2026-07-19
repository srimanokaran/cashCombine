package com.example.cashCombine.ledger.categorisation;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.imports.ParsedTransactionRow;
import com.example.cashCombine.ledger.transactions.InMemoryTransactionRepository;
import com.example.cashCombine.ledger.transactions.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoryReanalysisServiceTest {

	private InMemoryCategoryRepository categoryRepository;
	private InMemoryClassificationRuleRepository ruleRepository;
	private InMemoryTransactionRepository transactionRepository;
	private CategoryReanalysisService reanalysisService;
	private Category uncategorised;
	private Category fuel;
	private Category rent;

	@BeforeEach
	void setUp() {
		categoryRepository = new InMemoryCategoryRepository();
		ruleRepository = new InMemoryClassificationRuleRepository();
		transactionRepository = new InMemoryTransactionRepository();
		uncategorised = categoryRepository.save(Category.uncategorised());
		fuel = categoryRepository.save(Category.create("Fuel"));
		rent = categoryRepository.save(Category.create("Rent"));
		TransactionClassifier classifier = new TransactionClassifier(ruleRepository, uncategorised.id());
		reanalysisService = new CategoryReanalysisService(transactionRepository, classifier);
	}

	@Test
	void reassignsRuleBasedTransactionsAndSkipsManualOverrides() {
		ruleRepository.save(ClassificationRule.create("BP ", fuel.id()));
		ruleRepository.save(ClassificationRule.create("Transfer To Landlord", rent.id()));

		Transaction bpay = transactionRepository.save(tx("Qantas Credit Cards CommBank app BPAY Bill", fuel.id()));
		Transaction bpPetrol = transactionRepository.save(tx("BP EXPRESS HIGHWAY", uncategorised.id()));
		Transaction rentTransfer = transactionRepository.save(tx("Transfer To Landlord CommBank App Rent", uncategorised.id()));
		Transaction manual = transactionRepository.save(tx("Gift for friend", uncategorised.id()));
		manual.changeCategory(rent.id());
		transactionRepository.save(manual);

		CategoryReanalysisResult result = reanalysisService.reanalyse();

		assertThat(result.examined()).isEqualTo(4);
		assertThat(result.updated()).isEqualTo(3);
		assertThat(result.skippedManual()).isEqualTo(1);

		assertThat(transactionRepository.findById(bpay.id()).orElseThrow().categoryId())
				.isEqualTo(uncategorised.id());
		assertThat(transactionRepository.findById(bpPetrol.id()).orElseThrow().categoryId())
				.isEqualTo(fuel.id());
		assertThat(transactionRepository.findById(rentTransfer.id()).orElseThrow().categoryId()).isEqualTo(rent.id());
		assertThat(transactionRepository.findById(manual.id()).orElseThrow().categoryId()).isEqualTo(rent.id());
		assertThat(transactionRepository.findById(manual.id()).orElseThrow().isManuallyCategorised()).isTrue();
	}

	private Transaction tx(String description, CategoryId categoryId) {
		return Transaction.create(
				AccountId.generate(),
				new ParsedTransactionRow(
						LocalDate.of(2026, 7, 10),
						new BigDecimal("-50.00"),
						description,
						new BigDecimal("100.00")),
				categoryId);
	}

}
