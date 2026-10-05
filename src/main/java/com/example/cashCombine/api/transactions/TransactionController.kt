package com.example.cashCombine.api.transactions

import com.example.cashCombine.ledger.categorisation.CategoryId
import com.example.cashCombine.ledger.transactions.TransactionId
import com.example.cashCombine.ledger.transactions.TransactionService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/transactions")
class TransactionController(private val transactionService: TransactionService) {

    @PatchMapping("/{id}/category")
    fun changeCategory(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ChangeCategoryRequest,
    ): TransactionResponse =
        TransactionResponse.from(
            transactionService.changeCategory(TransactionId(id), CategoryId(request.categoryId))
        )
}