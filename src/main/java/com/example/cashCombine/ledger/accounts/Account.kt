package com.example.cashCombine.ledger.accounts

class Account private constructor(
    private val _id: AccountId,
    private val _name: String,
    private var _type: AccountType,
    private var _hasImports: Boolean,
) {

    fun id(): AccountId = _id

    fun name(): String = _name

    fun type(): AccountType = _type

    fun hasImports(): Boolean = _hasImports

    fun markAsImported() {
        _hasImports = true
    }

    fun clearImports() {
        _hasImports = false
    }

    fun changeType(newType: AccountType) {
        if (_hasImports) {
            throw IllegalStateException("Cannot change account type after imports exist")
        }
        requireNotNull(newType) { "Account type is required" }
        _type = newType
    }

    companion object {
        @JvmStatic
        fun create(name: String, type: AccountType): Account {
            require(name.isNotBlank()) { "Account name is required" }
            requireNotNull(type) { "Account type is required" }
            return Account(AccountId.generate(), name.trim(), type, false)
        }

        @JvmStatic
        fun reconstitute(id: AccountId, name: String, type: AccountType, hasImports: Boolean): Account {
            return Account(id, name, type, hasImports)
        }
    }
}