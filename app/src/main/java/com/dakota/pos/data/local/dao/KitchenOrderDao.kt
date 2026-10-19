package com.dakota.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.dakota.pos.data.local.entity.KitchenOrderEntity
import com.dakota.pos.data.local.entity.KitchenOrderItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KitchenOrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKitchenOrder(order: KitchenOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKitchenOrderItems(items: List<KitchenOrderItemEntity>)

    @Update
    suspend fun updateKitchenOrder(order: KitchenOrderEntity)

    @Transaction
    suspend fun saveFullKitchenOrder(order: KitchenOrderEntity, items: List<KitchenOrderItemEntity>) {
        insertKitchenOrder(order)
        insertKitchenOrderItems(items)
    }

    @Query("SELECT * FROM kitchen_orders WHERE id = :id LIMIT 1")
    suspend fun getKitchenOrderById(id: String): KitchenOrderEntity?

    @Query("SELECT * FROM kitchen_order_items WHERE kitchenOrderId = :kitchenOrderId")
    suspend fun getItemsForKitchenOrder(kitchenOrderId: String): List<KitchenOrderItemEntity>

    @Query("SELECT * FROM kitchen_orders WHERE status != 'SERVED' AND status != 'CANCELLED' ORDER BY orderTime ASC")
    fun getActiveKitchenOrdersFlow(): Flow<List<KitchenOrderEntity>>

    @Query("SELECT * FROM kitchen_orders WHERE stationId = :stationId AND status != 'SERVED' AND status != 'CANCELLED' ORDER BY orderTime ASC")
    fun getActiveKitchenOrdersByStationFlow(stationId: String): Flow<List<KitchenOrderEntity>>

    @Query("SELECT * FROM kitchen_orders ORDER BY orderTime DESC LIMIT 100")
    fun getAllKitchenOrdersFlow(): Flow<List<KitchenOrderEntity>>

    @Query("UPDATE kitchen_orders SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE kitchen_orders SET status = :status, acceptedTime = :time WHERE id = :id")
    suspend fun markAccepted(id: String, status: String, time: Long)

    @Query("UPDATE kitchen_orders SET status = :status, readyTime = :time WHERE id = :id")
    suspend fun markReady(id: String, status: String, time: Long)

    @Query("UPDATE kitchen_orders SET status = :status, servedTime = :time WHERE id = :id")
    suspend fun markServed(id: String, status: String, time: Long)
}
