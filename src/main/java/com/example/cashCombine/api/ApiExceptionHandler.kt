package com.example.cashCombine.api

import com.example.cashCombine.ledger.accounts.AccountNotFoundException
import com.example.cashCombine.ledger.categorisation.CategoryNotFoundException
import com.example.cashCombine.ledger.categorisation.DuplicateCategoryNameException
import com.example.cashCombine.ledger.categorisation.ProtectedCategoryException
import com.example.cashCombine.ledger.categorisation.RuleNotFoundException
import com.example.cashCombine.ledger.imports.ImportNotFoundException
import com.example.cashCombine.ledger.imports.InvalidCsvFormatException
import com.example.cashCombine.ledger.transactions.TransactionNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(
        AccountNotFoundException::class,
        CategoryNotFoundException::class,
        TransactionNotFoundException::class,
        RuleNotFoundException::class,
        ImportNotFoundException::class,
    )
    fun notFound(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(ex.message ?: ""))

    @ExceptionHandler(InvalidCsvFormatException::class, IllegalArgumentException::class)
    fun badRequest(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(ex.message ?: ""))

    @ExceptionHandler(DuplicateCategoryNameException::class, ProtectedCategoryException::class)
    fun conflict(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(ex.message ?: ""))

    @ExceptionHandler(IllegalStateException::class)
    fun illegalState(ex: IllegalStateException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(ex.message ?: ""))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = ex.bindingResult.fieldErrors.firstOrNull()
            ?.let { "${it.field}: ${it.defaultMessage}" }
            ?: "Validation failed"
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(message))
    }
}