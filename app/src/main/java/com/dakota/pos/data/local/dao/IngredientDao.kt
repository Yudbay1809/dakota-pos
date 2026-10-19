package com.dakota.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dakota.pos.data.local.entity.IngredientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: IngredientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Update
    suspend fun updateIngredient(ingredient: IngredientEntity)

    @Delete
    suspend fun deleteIngredient(ingredient: IngredientEntity)

    @Query("SELECT * FROM ingredients WHERE id = :id LIMIT 1")
    suspend fun getIngredientById(id: String): IngredientEntity?

    @Query("SELECT * FROM ingredients ORDER BY name ASC")
    fun getAllIngredientsFlow(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients ORDER BY name ASC")
    suspend fun getAllIngredients(): List<IngredientEntity>

    @Query("UPDATE ingredients SET currentStock = :newStock, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCurrentStock(id: String, newStock: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM ingredients")
    suspend fun getIngredientCount(): Int
}
