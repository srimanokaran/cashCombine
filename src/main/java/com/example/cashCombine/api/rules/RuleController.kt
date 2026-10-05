package com.example.cashCombine.api.rules

import com.example.cashCombine.ledger.categorisation.CategoryId
import com.example.cashCombine.ledger.categorisation.ClassificationRuleId
import com.example.cashCombine.ledger.categorisation.ClassificationRuleService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/rules")
class RuleController(private val ruleService: ClassificationRuleService) {

    @GetMapping
    fun list(): List<RuleResponse> =
        ruleService.listRules().map { RuleResponse.from(it) }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateRuleRequest): RuleResponse =
        RuleResponse.from(ruleService.createRule(request.pattern, CategoryId(request.categoryId)))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) {
        ruleService.deleteRule(ClassificationRuleId(id))
    }
}