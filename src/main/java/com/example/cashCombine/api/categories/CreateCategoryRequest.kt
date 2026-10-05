package com.example.cashCombine.api.categories

import jakarta.validation.constraints.NotBlank

data class CreateCategoryRequest(@field:NotBlank val name: String)