package com.example.cashCombine.api.search

import com.example.cashCombine.api.ErrorResponse
import com.example.cashCombine.ledger.search.AiSearchException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class AiExpenseSearchRequest(val query: String? = null)
data class AiAvailabilityResponse(val available: Boolean)

@RestControllerAdvice
class AiSearchExceptionHandler {
    @ExceptionHandler(AiSearchException::class)
    fun handle(ex: AiSearchException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(ex.status).body(ErrorResponse(ex.message ?: "AI search failed."))
}
