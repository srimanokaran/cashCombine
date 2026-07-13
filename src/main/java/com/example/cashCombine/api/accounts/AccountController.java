package com.example.cashCombine.api.accounts;

import com.example.cashCombine.api.transactions.TransactionResponse;
import com.example.cashCombine.ledger.accounts.AccountId;
import com.example.cashCombine.ledger.accounts.AccountService;
import com.example.cashCombine.ledger.imports.ImportResult;
import com.example.cashCombine.ledger.imports.ImportService;
import com.example.cashCombine.ledger.transactions.TransactionService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

	private final AccountService accountService;
	private final ImportService importService;
	private final TransactionService transactionService;

	public AccountController(
			AccountService accountService, ImportService importService, TransactionService transactionService) {
		this.accountService = accountService;
		this.importService = importService;
		this.transactionService = transactionService;
	}

	@GetMapping
	public List<AccountResponse> list() {
		return accountService.listAccounts().stream().map(AccountResponse::from).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
		return AccountResponse.from(accountService.createAccount(request.name(), request.type()));
	}

	@GetMapping("/{id}")
	public AccountResponse get(@PathVariable UUID id) {
		return AccountResponse.from(accountService.getAccount(new AccountId(id)));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID id) {
		accountService.deleteAccount(new AccountId(id));
	}

	@PostMapping(path = "/{id}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ImportResultResponse importCsv(@PathVariable UUID id, @RequestPart("file") MultipartFile file)
			throws IOException {
		ImportResult result = importService.importCsv(new AccountId(id), file.getInputStream());
		return ImportResultResponse.from(result);
	}

	@GetMapping("/{id}/transactions")
	public List<TransactionResponse> listTransactions(@PathVariable UUID id) {
		accountService.getAccount(new AccountId(id));
		return transactionService.listByAccount(new AccountId(id)).stream()
				.map(TransactionResponse::from)
				.toList();
	}

}
