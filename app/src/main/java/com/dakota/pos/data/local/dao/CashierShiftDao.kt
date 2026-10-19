package com.dakota.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dakota.pos.data.local.entity.CashierShiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashierShiftDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: CashierShiftEntity)

    @Update
    suspend fun updateShift(shift: CashierShiftEntity)

    @Query("SELECT * FROM cashier_shifts WHERE id = :id LIMIT 1")
    suspend fun getShiftById(id: String): CashierShiftEntity?

    @Query("SELECT * FROM cashier_shifts WHERE status = 'OPEN' AND cashierId = :cashierId LIMIT 1")
    suspend fun getActiveShiftForCashier(cashierId: String): CashierShiftEntity?

    @Query("SELECT * FROM cashier_shifts WHERE status = 'OPEN' LIMIT 1")
    suspend fun getAnyActiveShift(): CashierShiftEntity?

    @Query("SELECT * FROM cashier_shifts WHERE status = 'OPEN' LIMIT 1")
    fun getActiveShiftFlow(): Flow<CashierShiftEntity?>

    @Query("SELECT * FROM cashier_shifts ORDER BY startTime DESC")
    fun getAllShiftsFlow(): Flow<List<CashierShiftEntity>>

    @Query("SELECT * FROM cashier_shifts WHERE cashierId = :cashierId ORDER BY startTime DESC")
    fun getShiftsByCashierFlow(cashierId: String): Flow<List<CashierShiftEntity>>

    @Query("SELECT COUNT(*) FROM cashier_shifts")
    suspend fun getShiftCount(): Int
}
