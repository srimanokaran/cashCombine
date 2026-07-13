package com.example.cashCombine.api.accounts;

import com.example.cashCombine.ledger.accounts.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(@NotBlank String name, @NotNull AccountType type) {
}
