package com.dakota.pos.domain.usecase.purchasing

import com.dakota.pos.domain.model.recipe.Ingredient
import com.dakota.pos.domain.model.unit.UnitType
import com.dakota.pos.domain.recipe.UnitConverter
import com.dakota.pos.domain.repository.RecipeRepository

class UpdateIngredientCostUseCase(
    private val recipeRepository: RecipeRepository
) {

    /**
     * Menghitung dan memperbarui harga pokok (cost per default unit) bahan baku
     * menggunakan metode Moving Average Cost saat barang masuk.
     *
     * Formula Moving Average:
     * New Cost = ((Current Stock * Old Cost) + (Received Qty in Default Unit * New Unit Price in Default Unit))
     *            / (Current Stock + Received Qty in Default Unit)
     *
     * Jika Current Stock <= 0, maka New Cost = New Unit Price in Default Unit.
     */
    suspend operator fun invoke(
        ingredientId: String,
        receivedQuantity: Double,
        receivedUnit: UnitType,
        receivedUnitPrice: Double
    ): Result<Double> {
        val ingredient = recipeRepository.getIngredientById(ingredientId)
            ?: return Result.failure(IllegalArgumentException("Ingredient dengan ID $ingredientId tidak ditemukan"))

        val newCost = calculateNewMovingAverageCost(
            ingredient = ingredient,
            receivedQuantity = receivedQuantity,
            receivedUnit = receivedUnit,
            receivedUnitPrice = receivedUnitPrice
        )

        val updatedIngredient = ingredient.copy(
            costPerDefaultUnit = newCost,
            updatedAt = System.currentTimeMillis()
        )
        recipeRepository.saveIngredient(updatedIngredient)

        return Result.success(newCost)
    }

    fun calculateNewMovingAverageCost(
        ingredient: Ingredient,
        receivedQuantity: Double,
        receivedUnit: UnitType,
        receivedUnitPrice: Double
    ): Double {
        // Konversi kuantitas dan harga per unit ke default unit dari bahan baku
        val receivedQtyInDefaultUnit = if (UnitConverter.areCompatible(receivedUnit, ingredient.defaultUnit)) {
            UnitConverter.convert(receivedQuantity, receivedUnit, ingredient.defaultUnit)
        } else {
            receivedQuantity
        }

        val unitPriceInDefaultUnit = if (UnitConverter.areCompatible(receivedUnit, ingredient.defaultUnit)) {
            UnitConverter.convertCost(receivedUnitPrice, receivedUnit, ingredient.defaultUnit)
        } else {
            receivedUnitPrice
        }

        val currentStock = ingredient.currentStock

        if (currentStock <= 0.0 || ingredient.costPerDefaultUnit <= 0.0) {
            return unitPriceInDefaultUnit
        }

        val currentValue = currentStock * ingredient.costPerDefaultUnit
        val incomingValue = receivedQtyInDefaultUnit * unitPriceInDefaultUnit
        val totalQty = currentStock + receivedQtyInDefaultUnit

        return if (totalQty > 0.0) {
            (currentValue + incomingValue) / totalQty
        } else {
            unitPriceInDefaultUnit
        }
    }
}
