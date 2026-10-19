package com.dakota.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dakota.pos.data.local.entity.CashMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashMovementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: CashMovementEntity)

    @Query("SELECT * FROM cash_movements WHERE shiftId = :shiftId ORDER BY timestamp ASC")
    fun getMovementsForShiftFlow(shiftId: String): Flow<List<CashMovementEntity>>

    @Query("SELECT * FROM cash_movements WHERE shiftId = :shiftId ORDER BY timestamp ASC")
    suspend fun getMovementsForShift(shiftId: String): List<CashMovementEntity>

    @Query("SELECT * FROM cash_movements ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMovementsFlow(limit: Int = 100): Flow<List<CashMovementEntity>>
}
