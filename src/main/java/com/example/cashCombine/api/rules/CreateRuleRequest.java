package com.example.cashCombine.api.rules;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateRuleRequest(@NotBlank String pattern, @NotNull UUID categoryId) {
}
