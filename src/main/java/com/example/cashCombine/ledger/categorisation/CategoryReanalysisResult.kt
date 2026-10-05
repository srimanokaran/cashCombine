package com.example.cashCombine.ledger.categorisation

data class CategoryReanalysisResult(
    @get:JvmName("examined") val examined: Int,
    @get:JvmName("updated") val updated: Int,
    @get:JvmName("skippedManual") val skippedManual: Int)