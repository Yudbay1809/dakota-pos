package com.dakota.pos.domain.model.finance

data class ProductSalesMetric(
    val productId: String,
    val productName: String,
    val quantitySold: Int,
    val revenue: Double,
    val cogs: Double,
    val grossProfit: Double,
    val marginPercent: Double
)
