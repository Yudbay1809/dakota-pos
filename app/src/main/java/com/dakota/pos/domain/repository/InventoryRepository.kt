package com.dakota.pos.domain.repository

import com.dakota.pos.domain.model.inventory.MovementType
import com.dakota.pos.domain.model.inventory.StockMovement
import com.dakota.pos.domain.model.inventory.StockOpname
import com.dakota.pos.domain.model.recipe.Ingredient
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    // Stock Movements
    suspend fun recordMovement(movement: StockMovement)
    suspend fun recordMovements(movements: List<StockMovement>)
    fun getMovementsForIngredient(ingredientId: String): Flow<List<StockMovement>>
    fun getRecentMovements(limit: Int = 100): Flow<List<StockMovement>>
    fun getMovementsByType(type: MovementType): Flow<List<StockMovement>>
    suspend fun getMovementsByReferenceId(referenceId: String): List<StockMovement>
    suspend fun hasBeenDeductedForOrder(orderId: String): Boolean

    // Stock Opname
    suspend fun saveStockOpname(opname: StockOpname)
    fun getAllOpnamesFlow(): Flow<List<StockOpname>>

    // Ingredient Stock Updates
    suspend fun updateIngredientStock(ingredientId: String, newStock: Double)
    suspend fun getIngredientById(ingredientId: String): Ingredient?
    suspend fun getAllIngredients(): List<Ingredient>
    fun getAllIngredientsFlow(): Flow<List<Ingredient>>
}
