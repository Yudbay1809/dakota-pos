package com.dakota.pos.domain.recipe

import com.dakota.pos.domain.model.recipe.HppBreakdown
import com.dakota.pos.domain.model.recipe.Ingredient
import com.dakota.pos.domain.model.recipe.Recipe
import com.dakota.pos.domain.model.recipe.RecipeItemHpp

/**
 * Engine penghitung Harga Pokok Penjualan (HPP) berdasarkan komposisi resep
 * dan harga perolehan bahan baku mentah (Ingredient Cost).
 */
object HppCalculator {

    fun calculate(
        recipe: Recipe,
        ingredientsMap: Map<String, Ingredient>,
        sellingPrice: Double
    ): HppBreakdown {
        val breakdowns = mutableListOf<RecipeItemHpp>()
        var totalHpp = 0.0

        for (item in recipe.items) {
            val ingredient = ingredientsMap[item.ingredientId]
            if (ingredient != null) {
                val costPerRecipeUnit = UnitConverter.convertCost(
                    costPerOriginalUnit = ingredient.costPerDefaultUnit,
                    originalUnit = ingredient.defaultUnit,
                    targetUnit = item.unit
                )
                val itemSubtotal = item.quantity * costPerRecipeUnit
                totalHpp += itemSubtotal

                breakdowns.add(
                    RecipeItemHpp(
                        ingredientId = ingredient.id,
                        ingredientName = ingredient.name,
                        recipeQuantity = item.quantity,
                        recipeUnit = item.unit,
                        ingredientBaseUnit = ingredient.defaultUnit,
                        ingredientBaseCost = ingredient.costPerDefaultUnit,
                        costPerRecipeUnit = costPerRecipeUnit,
                        subtotalHpp = itemSubtotal
                    )
                )
            } else {
                // Jika bahan baku tidak ditemukan di katalog, catat dengan biaya 0
                breakdowns.add(
                    RecipeItemHpp(
                        ingredientId = item.ingredientId,
                        ingredientName = item.ingredientName,
                        recipeQuantity = item.quantity,
                        recipeUnit = item.unit,
                        ingredientBaseUnit = item.unit,
                        ingredientBaseCost = 0.0,
                        costPerRecipeUnit = 0.0,
                        subtotalHpp = 0.0
                    )
                )
            }
        }

        val grossProfit = sellingPrice - totalHpp
        val grossMarginPercentage = if (sellingPrice > 0) {
            (grossProfit / sellingPrice) * 100.0
        } else {
            0.0
        }

        return HppBreakdown(
            productId = recipe.productId,
            productName = recipe.productName,
            variantId = recipe.variantId,
            variantName = recipe.variantName,
            sellingPrice = sellingPrice,
            totalHpp = totalHpp,
            grossProfit = grossProfit,
            grossMarginPercentage = grossMarginPercentage,
            itemBreakdowns = breakdowns
        )
    }
}
