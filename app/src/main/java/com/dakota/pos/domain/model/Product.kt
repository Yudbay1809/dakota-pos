package com.dakota.pos.domain.model

data class Product(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String = "",
    val basePrice: Double,
    val colorHex: String = "#6F4E37",
    val isActive: Boolean = true,
    val variants: List<ProductVariant> = emptyList()
)
