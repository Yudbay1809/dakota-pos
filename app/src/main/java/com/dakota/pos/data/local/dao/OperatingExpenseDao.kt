package com.dakota.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dakota.pos.data.local.entity.OperatingExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OperatingExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: OperatingExpenseEntity)

    @Update
    suspend fun updateExpense(expense: OperatingExpenseEntity)

    @Query("DELETE FROM operating_expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: String)

    @Query("SELECT * FROM operating_expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: String): OperatingExpenseEntity?

    @Query("SELECT * FROM operating_expenses WHERE date >= :startTime AND date <= :endTime ORDER BY date DESC")
    suspend fun getExpensesBetween(startTime: Long, endTime: Long): List<OperatingExpenseEntity>

    @Query("SELECT * FROM operating_expenses WHERE date >= :startTime AND date <= :endTime ORDER BY date DESC")
    fun getExpensesBetweenFlow(startTime: Long, endTime: Long): Flow<List<OperatingExpenseEntity>>

    @Query("SELECT * FROM operating_expenses ORDER BY date DESC")
    fun getAllExpensesFlow(): Flow<List<OperatingExpenseEntity>>

    @Query("SELECT SUM(amount) FROM operating_expenses WHERE date >= :startTime AND date <= :endTime")
    suspend fun getTotalExpenseBetween(startTime: Long, endTime: Long): Double?
}
