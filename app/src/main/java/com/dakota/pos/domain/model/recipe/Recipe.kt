package com.dakota.pos.domain.model.recipe

/**
 * Resep Produk / Varian.
 * Sesuai Cardinal Rule 13: Recipe menghubungkan Product dengan satu atau lebih Ingredient.
 */
data class Recipe(
    val id: String,
    val productId: String,
    val productName: String,
    val variantId: String? = null,
    val variantName: String? = null,
    val items: List<RecipeItem> = emptyList(),
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
