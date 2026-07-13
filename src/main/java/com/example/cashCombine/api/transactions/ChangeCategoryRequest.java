package com.example.cashCombine.api.transactions;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChangeCategoryRequest(@NotNull UUID categoryId) {
}
