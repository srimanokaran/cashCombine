package com.example.cashCombine.api.accounts

import com.example.cashCombine.ledger.accounts.Account
import com.example.cashCombine.ledger.accounts.AccountType
import java.util.UUID

data class AccountResponse(val id: UUID, val name: String, val type: AccountType, val hasImports: Boolean) {

    companion object {
        fun from(account: Account): AccountResponse =
            AccountResponse(account.id().value, account.name(), account.type(), account.hasImports())
    }
}