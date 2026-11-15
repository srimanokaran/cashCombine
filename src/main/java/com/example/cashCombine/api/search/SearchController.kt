package com.example.cashCombine.api.search

import com.example.cashCombine.ledger.dashboard.DateWindow
import com.example.cashCombine.ledger.search.ExpenseSearchService
import com.example.cashCombine.ledger.search.AiExpenseSearchService
import com.example.cashCombine.ledger.search.SearchTokens
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/search")
class SearchController(
    private val expenseSearchService: ExpenseSearchService,
    private val aiExpenseSearchService: AiExpenseSearchService,
) {

    @PostMapping("/ai/start")
    fun startAi(): AiAvailabilityResponse {
        aiExpenseSearchService.start()
        return AiAvailabilityResponse(true)
    }

    @PostMapping("/expenses/ai")
    fun aiExpenses(@RequestBody request: AiExpenseSearchRequest): ExpenseSearchResponse =
        ExpenseSearchResponse.from(aiExpenseSearchService.search(request.query ?: ""))

    @GetMapping("/expenses")
    fun expenses(
        @RequestParam q: String,
        @RequestParam(required = false) expand: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        @RequestParam(defaultValue = "${ExpenseSearchService.DEFAULT_LIMIT}") limit: Int,
    ): ExpenseSearchResponse {
        val expandedTerms = expand
            ?.split(",")
            ?.let { SearchTokens.sanitise(it) }
            ?: emptyList()
        return ExpenseSearchResponse.from(
            expenseSearchService.search(q, window(from, to), expandedTerms, limit))
    }

    @GetMapping("/expenses/expand")
    fun expand(@RequestParam q: String): QueryExpansionResponse {
        val expansion = expenseSearchService.expand(q)
        return QueryExpansionResponse(expansion.available, expansion.query, expansion.terms)
    }

    private fun window(from: LocalDate?, to: LocalDate?): DateWindow =
        if (from == null && to == null) DateWindow.ALL else DateWindow(from, to)
}
