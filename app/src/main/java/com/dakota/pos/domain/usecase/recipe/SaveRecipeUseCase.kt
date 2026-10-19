package com.dakota.pos.domain.usecase.recipe

import com.dakota.pos.domain.model.recipe.Recipe
import com.dakota.pos.domain.repository.RecipeRepository

class SaveRecipeUseCase(
    private val recipeRepository: RecipeRepository
) {
    suspend operator fun invoke(recipe: Recipe): Result<Unit> {
        if (recipe.items.isEmpty()) {
            return Result.failure(IllegalArgumentException("Resep wajib memiliki minimal satu bahan baku."))
        }
        for (item in recipe.items) {
            if (item.quantity <= 0) {
                return Result.failure(IllegalArgumentException("Kuantitas bahan ${item.ingredientName} harus lebih dari 0."))
            }
        }
        return try {
            recipeRepository.saveRecipe(recipe)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
