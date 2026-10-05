package com.example.cashCombine.api.dashboard

import com.example.cashCombine.ledger.categorisation.CategoryId
import com.example.cashCombine.ledger.categorisation.CategoryReanalysisService
import com.example.cashCombine.ledger.dashboard.DashboardService
import com.example.cashCombine.ledger.dashboard.DateWindow
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val dashboardService: DashboardService,
    private val categoryReanalysisService: CategoryReanalysisService,
) {

    @GetMapping("/expenses")
    fun expenses(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
    ): ExpenseDashboardResponse =
        ExpenseDashboardResponse.from(dashboardService.expenseBreakdown(window(from, to)))

    @GetMapping("/monthly")
    fun monthly(): List<MonthlyCashflowResponse> =
        dashboardService.monthlyCashflow().map { MonthlyCashflowResponse.from(it) }

    @GetMapping("/expenses/categories/{categoryId}/transactions")
    fun expenseTransactions(
        @PathVariable categoryId: UUID,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
    ): List<ExpenseTransactionResponse> =
        dashboardService.expenseTransactions(CategoryId(categoryId), window(from, to))
            .map { ExpenseTransactionResponse.from(it) }

    @GetMapping("/income/categories/{categoryId}/transactions")
    fun incomeTransactions(
        @PathVariable categoryId: UUID,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
    ): List<ExpenseTransactionResponse> =
        dashboardService.incomeTransactions(CategoryId(categoryId), window(from, to))
            .map { ExpenseTransactionResponse.from(it) }

    @PostMapping("/expenses/reanalyse")
    fun reanalyse(): CategoryReanalysisResponse =
        CategoryReanalysisResponse.from(categoryReanalysisService.reanalyse())

    private fun window(from: LocalDate?, to: LocalDate?): DateWindow =
        if (from == null && to == null) DateWindow.ALL else DateWindow(from, to)
}