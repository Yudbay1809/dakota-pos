package com.dakota.pos.domain.model

data class ProductVariant(
    val id: String,
    val productId: String,
    val name: String,
    val priceAdjustment: Double = 0.0,
    val isDefault: Boolean = false
)
