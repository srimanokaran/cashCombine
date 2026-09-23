package com.example.cashCombine.api.dashboard;

import com.example.cashCombine.ledger.categorisation.CategoryId;
import com.example.cashCombine.ledger.categorisation.CategoryReanalysisService;
import com.example.cashCombine.ledger.dashboard.DashboardService;
import com.example.cashCombine.ledger.dashboard.DateWindow;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

	private final DashboardService dashboardService;
	private final CategoryReanalysisService categoryReanalysisService;

	public DashboardController(
			DashboardService dashboardService, CategoryReanalysisService categoryReanalysisService) {
		this.dashboardService = dashboardService;
		this.categoryReanalysisService = categoryReanalysisService;
	}

	@GetMapping("/expenses")
	public ExpenseDashboardResponse expenses(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return ExpenseDashboardResponse.from(dashboardService.expenseBreakdown(window(from, to)));
	}

	@GetMapping("/monthly")
	public List<MonthlyCashflowResponse> monthly() {
		return dashboardService.monthlyCashflow().stream().map(MonthlyCashflowResponse::from).toList();
	}

	@GetMapping("/expenses/categories/{categoryId}/transactions")
	public List<ExpenseTransactionResponse> expenseTransactions(
			@PathVariable UUID categoryId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return dashboardService.expenseTransactions(new CategoryId(categoryId), window(from, to)).stream()
				.map(ExpenseTransactionResponse::from)
				.toList();
	}

	@GetMapping("/income/categories/{categoryId}/transactions")
	public List<ExpenseTransactionResponse> incomeTransactions(
			@PathVariable UUID categoryId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return dashboardService.incomeTransactions(new CategoryId(categoryId), window(from, to)).stream()
				.map(ExpenseTransactionResponse::from)
				.toList();
	}

	@PostMapping("/expenses/reanalyse")
	public CategoryReanalysisResponse reanalyse() {
		return CategoryReanalysisResponse.from(categoryReanalysisService.reanalyse());
	}

	private static DateWindow window(LocalDate from, LocalDate to) {
		if (from == null && to == null) {
			return DateWindow.ALL;
		}
		return new DateWindow(from, to);
	}

}
