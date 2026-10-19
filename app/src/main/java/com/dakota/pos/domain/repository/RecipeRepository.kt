package com.dakota.pos.domain.repository

import com.dakota.pos.domain.model.recipe.HppBreakdown
import com.dakota.pos.domain.model.recipe.Ingredient
import com.dakota.pos.domain.model.recipe.Recipe
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    fun getAllIngredientsFlow(): Flow<List<Ingredient>>
    suspend fun getAllIngredients(): List<Ingredient>
    suspend fun getIngredientById(id: String): Ingredient?
    suspend fun saveIngredient(ingredient: Ingredient)
    suspend fun deleteIngredient(ingredient: Ingredient)

    fun getAllRecipesFlow(): Flow<List<Recipe>>
    suspend fun getRecipeForProduct(productId: String, variantId: String? = null): Recipe?
    suspend fun getRecipeById(recipeId: String): Recipe?
    suspend fun saveRecipe(recipe: Recipe)
    suspend fun deleteRecipe(recipeId: String)

    suspend fun calculateHppForProduct(productId: String, variantId: String?, sellingPrice: Double): HppBreakdown?
    suspend fun seedInitialIngredientsAndRecipesIfEmpty()
}
