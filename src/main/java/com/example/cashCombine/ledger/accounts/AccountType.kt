package com.example.cashCombine.ledger.accounts

enum class AccountType(@get:JvmName("displayName") val displayName: String) {
    COMMBANK("CommBank"),
    ING("ING"),
    NAB_CREDIT_CARD("NAB credit card"),
}