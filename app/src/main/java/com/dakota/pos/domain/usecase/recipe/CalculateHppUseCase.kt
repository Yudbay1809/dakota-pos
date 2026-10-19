package com.dakota.pos.domain.usecase.recipe

import com.dakota.pos.domain.model.recipe.HppBreakdown
import com.dakota.pos.domain.repository.RecipeRepository

/**
 * UseCase untuk menghitung Harga Pokok Penjualan (HPP / COGS) dan margin keuntungan produk.
 */
class CalculateHppUseCase(
    private val recipeRepository: RecipeRepository
) {
    suspend operator fun invoke(
        productId: String,
        variantId: String? = null,
        sellingPrice: Double
    ): HppBreakdown? {
        return recipeRepository.calculateHppForProduct(productId, variantId, sellingPrice)
    }
}
