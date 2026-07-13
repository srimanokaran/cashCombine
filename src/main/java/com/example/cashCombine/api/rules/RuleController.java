package com.example.cashCombine.api.rules;

import com.example.cashCombine.ledger.categorisation.CategoryId;
import com.example.cashCombine.ledger.categorisation.ClassificationRuleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rules")
public class RuleController {

	private final ClassificationRuleService ruleService;

	public RuleController(ClassificationRuleService ruleService) {
		this.ruleService = ruleService;
	}

	@GetMapping
	public List<RuleResponse> list() {
		return ruleService.listRules().stream().map(RuleResponse::from).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public RuleResponse create(@Valid @RequestBody CreateRuleRequest request) {
		return RuleResponse.from(ruleService.createRule(request.pattern(), new CategoryId(request.categoryId())));
	}

}
