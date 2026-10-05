package com.example.cashCombine.ledger.categorisation

class RuleNotFoundException(id: ClassificationRuleId) : RuntimeException("Classification rule not found: ${id.value}")