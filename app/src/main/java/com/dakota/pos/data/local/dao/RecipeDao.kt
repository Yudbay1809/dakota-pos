package com.dakota.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.dakota.pos.data.local.entity.RecipeEntity
import com.dakota.pos.data.local.entity.RecipeItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipeItems(items: List<RecipeItemEntity>)

    @Query("SELECT * FROM recipes WHERE productId = :productId AND (variantId = :variantId OR (:variantId IS NULL AND variantId IS NULL)) LIMIT 1")
    suspend fun getRecipeForProduct(productId: String, variantId: String?): RecipeEntity?

    @Query("SELECT * FROM recipes WHERE productId = :productId LIMIT 1")
    suspend fun getDefaultRecipeForProduct(productId: String): RecipeEntity?

    @Query("SELECT * FROM recipes WHERE id = :recipeId LIMIT 1")
    suspend fun getRecipeById(recipeId: String): RecipeEntity?

    @Query("SELECT * FROM recipe_items WHERE recipeId = :recipeId")
    suspend fun getItemsForRecipe(recipeId: String): List<RecipeItemEntity>

    @Query("SELECT * FROM recipes ORDER BY productName ASC")
    fun getAllRecipesFlow(): Flow<List<RecipeEntity>>

    @Query("DELETE FROM recipes WHERE id = :recipeId")
    suspend fun deleteRecipe(recipeId: String)

    @Query("DELETE FROM recipe_items WHERE recipeId = :recipeId")
    suspend fun deleteRecipeItems(recipeId: String)

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun getRecipeCount(): Int

    @Transaction
    suspend fun saveFullRecipe(recipe: RecipeEntity, items: List<RecipeItemEntity>) {
        deleteRecipeItems(recipe.id)
        insertRecipe(recipe)
        insertRecipeItems(items)
    }
}
