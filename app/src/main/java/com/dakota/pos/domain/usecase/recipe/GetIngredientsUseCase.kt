package com.dakota.pos.domain.usecase.recipe

import com.dakota.pos.domain.model.recipe.Ingredient
import com.dakota.pos.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow

class GetIngredientsUseCase(
    private val recipeRepository: RecipeRepository
) {
    operator fun invoke(): Flow<List<Ingredient>> {
        return recipeRepository.getAllIngredientsFlow()
    }
}
