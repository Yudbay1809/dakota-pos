package com.dakota.pos.domain.usecase.recipe

import com.dakota.pos.domain.model.recipe.Ingredient
import com.dakota.pos.domain.repository.RecipeRepository

class SaveIngredientUseCase(
    private val recipeRepository: RecipeRepository
) {
    suspend operator fun invoke(ingredient: Ingredient): Result<Unit> {
        if (ingredient.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama bahan baku tidak boleh kosong."))
        }
        if (ingredient.costPerDefaultUnit < 0) {
            return Result.failure(IllegalArgumentException("Biaya per satuan tidak boleh negatif."))
        }
        return try {
            recipeRepository.saveIngredient(ingredient)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
