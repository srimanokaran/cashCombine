package com.example.cashCombine.api.categories

import com.example.cashCombine.ledger.categorisation.Category
import java.util.UUID

data class CategoryResponse(val id: UUID, val name: String) {

    companion object {
        fun from(category: Category): CategoryResponse =
            CategoryResponse(category.id.value, category.name)
    }
}