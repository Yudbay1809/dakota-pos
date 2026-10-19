package com.dakota.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dakota.pos.data.local.entity.StationConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StationConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: StationConfigEntity)

    @Query("SELECT * FROM station_configs")
    fun getAllConfigsFlow(): Flow<List<StationConfigEntity>>

    @Query("SELECT * FROM station_configs")
    suspend fun getAllConfigs(): List<StationConfigEntity>

    @Query("SELECT * FROM station_configs WHERE stationId = :stationId LIMIT 1")
    suspend fun getConfigById(stationId: String): StationConfigEntity?

    @Query("SELECT COUNT(*) FROM station_configs")
    suspend fun getConfigCount(): Int
}
