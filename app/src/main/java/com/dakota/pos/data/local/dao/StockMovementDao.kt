package com.dakota.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dakota.pos.data.local.entity.StockMovementEntity
import com.dakota.pos.domain.model.inventory.MovementType
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<StockMovementEntity>)

    @Query("SELECT * FROM stock_movements WHERE ingredientId = :ingredientId ORDER BY timestamp DESC")
    fun getMovementsForIngredient(ingredientId: String): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMovements(limit: Int = 100): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE type = :type ORDER BY timestamp DESC")
    fun getMovementsByType(type: MovementType): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE referenceId = :referenceId ORDER BY timestamp ASC")
    suspend fun getMovementsByReferenceId(referenceId: String): List<StockMovementEntity>

    /**
     * Idempotency check: mencegah double-deduction jika orderId yang sama sudah pernah diproses.
     * Hanya cek SALE type karena RETURN bisa terjadi pada referenceId yang sama.
     */
    @Query("SELECT COUNT(*) FROM stock_movements WHERE referenceId = :orderId AND type = 'SALE'")
    suspend fun countSaleMovementsForOrder(orderId: String): Int

    @Query("SELECT * FROM stock_movements WHERE id = :id LIMIT 1")
    suspend fun getMovementById(id: String): StockMovementEntity?

    @Query("SELECT COUNT(*) FROM stock_movements")
    suspend fun getMovementCount(): Int
}
