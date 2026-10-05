package com.example.cashCombine.api.rules

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class CreateRuleRequest(@field:NotBlank val pattern: String, @field:NotNull val categoryId: UUID)