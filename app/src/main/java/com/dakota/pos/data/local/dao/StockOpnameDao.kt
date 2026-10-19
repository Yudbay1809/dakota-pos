package com.dakota.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.dakota.pos.data.local.entity.StockOpnameEntity
import com.dakota.pos.data.local.entity.StockOpnameItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockOpnameDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpname(opname: StockOpnameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpnameItems(items: List<StockOpnameItemEntity>)

    @Query("SELECT * FROM stock_opnames ORDER BY timestamp DESC")
    fun getAllOpnamesFlow(): Flow<List<StockOpnameEntity>>

    @Query("SELECT * FROM stock_opnames WHERE id = :id LIMIT 1")
    suspend fun getOpnameById(id: String): StockOpnameEntity?

    @Query("SELECT * FROM stock_opname_items WHERE opnameId = :opnameId")
    suspend fun getItemsForOpname(opnameId: String): List<StockOpnameItemEntity>

    @Transaction
    suspend fun saveFullOpname(opname: StockOpnameEntity, items: List<StockOpnameItemEntity>) {
        insertOpname(opname)
        insertOpnameItems(items)
    }
}
