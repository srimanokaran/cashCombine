package com.example.cashCombine.ledger.categorisation

class Category private constructor(
    @get:JvmName("id") val id: CategoryId,
    @get:JvmName("name") val name: String
) {

    val isUncategorised: Boolean get() = UNCATEGORISED_NAME.equals(name, ignoreCase = true)
    val isIncome: Boolean get() = INCOME_NAME.equals(name, ignoreCase = true)
    val isIncomeCreditCategory: Boolean get() = isIncome || isUncategorised
    val isExcludedFromExpenses: Boolean get() = FUNDS_BETWEEN_ACCOUNTS_NAME.equals(name, ignoreCase = true)

    companion object {
        const val UNCATEGORISED_NAME = "Uncategorised"
        const val FUNDS_BETWEEN_ACCOUNTS_NAME = "Funds between accounts"
        const val INCOME_NAME = "Income"

        @JvmStatic
        fun create(name: String): Category {
            require(name.isNotBlank()) { "Category name is required" }
            return Category(CategoryId.generate(), name.trim())
        }

        @JvmStatic
        fun reconstitute(id: CategoryId, name: String): Category = Category(id, name)

        @JvmStatic
        fun uncategorised(): Category = create(UNCATEGORISED_NAME)
    }
}