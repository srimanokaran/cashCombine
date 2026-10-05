package com.example.cashCombine.api.accounts

import com.example.cashCombine.ledger.accounts.AccountType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateAccountRequest(@field:NotBlank val name: String, @field:NotNull val type: AccountType)