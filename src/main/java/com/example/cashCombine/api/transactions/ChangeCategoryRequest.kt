package com.example.cashCombine.api.transactions

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class ChangeCategoryRequest(@field:NotNull val categoryId: UUID)