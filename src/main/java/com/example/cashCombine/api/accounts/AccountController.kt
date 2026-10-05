package com.example.cashCombine.api.accounts

import com.example.cashCombine.api.transactions.TransactionResponse
import com.example.cashCombine.ledger.accounts.AccountId
import com.example.cashCombine.ledger.accounts.AccountService
import com.example.cashCombine.ledger.imports.ImportBatchId
import com.example.cashCombine.ledger.imports.ImportService
import com.example.cashCombine.ledger.transactions.TransactionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.util.UUID

@RestController
@RequestMapping("/api/accounts")
class AccountController(
    private val accountService: AccountService,
    private val importService: ImportService,
    private val transactionService: TransactionService,
) {

    @GetMapping
    fun list(): List<AccountResponse> =
        accountService.listAccounts().map { AccountResponse.from(it) }

    @PostMapping("/ensure-fixed")
    fun ensureFixed(): List<AccountResponse> =
        accountService.ensureFixedAccounts().map { AccountResponse.from(it) }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateAccountRequest): AccountResponse =
        AccountResponse.from(accountService.createAccount(request.name, request.type))

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): AccountResponse =
        AccountResponse.from(accountService.getAccount(AccountId(id)))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) {
        accountService.deleteAccount(AccountId(id))
    }

    @PostMapping(path = ["/{id}/import"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Throws(IOException::class)
    fun importCsv(@PathVariable id: UUID, @RequestPart("file") file: MultipartFile): ImportResultResponse {
        val result = importService.importCsv(AccountId(id), file.inputStream, file.originalFilename)
        return ImportResultResponse.from(result)
    }

    @GetMapping("/{id}/imports")
    fun listImports(@PathVariable id: UUID): List<ImportBatchResponse> =
        importService.listImports(AccountId(id)).map { ImportBatchResponse.from(it) }

    @DeleteMapping("/{id}/imports/{importId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteImport(@PathVariable id: UUID, @PathVariable importId: UUID) {
        importService.deleteImport(AccountId(id), ImportBatchId(importId))
    }

    @GetMapping("/{id}/transactions")
    fun listTransactions(@PathVariable id: UUID): List<TransactionResponse> {
        accountService.getAccount(AccountId(id))
        return transactionService.listByAccount(AccountId(id))
            .map { TransactionResponse.from(it) }
    }
}