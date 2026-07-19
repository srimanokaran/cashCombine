package com.example.cashCombine.api.dashboard;

import com.example.cashCombine.ledger.categorisation.CategoryId;
import com.example.cashCombine.ledger.categorisation.CategoryReanalysisService;
import com.example.cashCombine.ledger.dashboard.DashboardService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
	public ExpenseDashboardResponse expenses() {
		return ExpenseDashboardResponse.from(dashboardService.expenseBreakdown());
	}

	@GetMapping("/expenses/categories/{categoryId}/transactions")
	public List<ExpenseTransactionResponse> expenseTransactions(@PathVariable UUID categoryId) {
		return dashboardService.expenseTransactions(new CategoryId(categoryId)).stream()
				.map(ExpenseTransactionResponse::from)
				.toList();
	}

	@PostMapping("/expenses/reanalyse")
	public CategoryReanalysisResponse reanalyse() {
		return CategoryReanalysisResponse.from(categoryReanalysisService.reanalyse());
	}

}
