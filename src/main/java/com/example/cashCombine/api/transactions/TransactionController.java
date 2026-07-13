package com.example.cashCombine.api.transactions;

import com.example.cashCombine.ledger.categorisation.CategoryId;
import com.example.cashCombine.ledger.transactions.TransactionId;
import com.example.cashCombine.ledger.transactions.TransactionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

	private final TransactionService transactionService;

	public TransactionController(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	@PatchMapping("/{id}/category")
	public TransactionResponse changeCategory(
			@PathVariable UUID id, @Valid @RequestBody ChangeCategoryRequest request) {
		return TransactionResponse.from(transactionService.changeCategory(
				new TransactionId(id), new CategoryId(request.categoryId())));
	}

}
