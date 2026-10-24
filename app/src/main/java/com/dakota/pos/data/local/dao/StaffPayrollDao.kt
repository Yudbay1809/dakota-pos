package com.dakota.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dakota.pos.data.local.entity.StaffPayrollEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffPayrollDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payroll: StaffPayrollEntity)

    @Query("SELECT * FROM staff_payroll ORDER BY generatedAt DESC")
    fun getAllPayrollsFlow(): Flow<List<StaffPayrollEntity>>

    @Query("SELECT * FROM staff_payroll WHERE periodMonth = :period ORDER BY generatedAt DESC")
    suspend fun getPayrollsByPeriod(period: String): List<StaffPayrollEntity>

    @Query("SELECT * FROM staff_payroll WHERE userId = :userId AND periodMonth = :period LIMIT 1")
    suspend fun getPayrollByUserAndPeriod(userId: String, period: String): StaffPayrollEntity?
}
