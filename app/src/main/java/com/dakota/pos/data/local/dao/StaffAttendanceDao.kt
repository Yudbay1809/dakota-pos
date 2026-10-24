package com.dakota.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dakota.pos.data.local.entity.StaffAttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffAttendanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attendance: StaffAttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attendances: List<StaffAttendanceEntity>)

    @Query("SELECT * FROM staff_attendance ORDER BY timestamp DESC")
    fun getAllAttendancesFlow(): Flow<List<StaffAttendanceEntity>>

    @Query("SELECT * FROM staff_attendance ORDER BY timestamp DESC")
    suspend fun getAllAttendances(): List<StaffAttendanceEntity>

    @Query("SELECT * FROM staff_attendance WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAttendancesByUser(userId: String): List<StaffAttendanceEntity>

    @Query("SELECT * FROM staff_attendance WHERE userId = :userId AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    suspend fun getAttendancesByUserAndPeriod(userId: String, startTime: Long, endTime: Long): List<StaffAttendanceEntity>

    @Query("DELETE FROM staff_attendance WHERE id = :id")
    suspend fun deleteById(id: String)
}
