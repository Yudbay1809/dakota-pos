package com.dakota.pos.domain.model.recipe

import com.dakota.pos.domain.model.unit.UnitType

data class RecipeItemHpp(
    val ingredientId: String,
    val ingredientName: String,
    val recipeQuantity: Double,
    val recipeUnit: UnitType,
    val ingredientBaseUnit: UnitType,
    val ingredientBaseCost: Double,
    val costPerRecipeUnit: Double,
    val subtotalHpp: Double
)

/**
 * Rincian kalkulasi Harga Pokok Penjualan (HPP / COGS) dan Margin Produk.
 * Sesuai Cardinal Rule 14: HPP dihitung dari Recipe + Ingredient Cost, bukan hardcoded.
 */
data class HppBreakdown(
    val productId: String,
    val productName: String,
    val variantId: String? = null,
    val variantName: String? = null,
    val sellingPrice: Double,
    val totalHpp: Double,
    val grossProfit: Double,
    val grossMarginPercentage: Double,
    val itemBreakdowns: List<RecipeItemHpp>
)
