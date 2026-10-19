package com.dakota.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.dakota.pos.data.local.entity.PurchaseOrderEntity
import com.dakota.pos.data.local.entity.PurchaseOrderItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseOrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseOrder(order: PurchaseOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseOrderItems(items: List<PurchaseOrderItemEntity>)

    @Query("SELECT * FROM purchase_orders ORDER BY createdAt DESC")
    fun getAllPurchaseOrdersFlow(): Flow<List<PurchaseOrderEntity>>

    @Query("SELECT * FROM purchase_orders WHERE status = :status ORDER BY createdAt DESC")
    fun getPurchaseOrdersByStatusFlow(status: String): Flow<List<PurchaseOrderEntity>>

    @Query("SELECT * FROM purchase_orders WHERE id = :id LIMIT 1")
    suspend fun getPurchaseOrderById(id: String): PurchaseOrderEntity?

    @Query("SELECT * FROM purchase_order_items WHERE purchaseOrderId = :orderId")
    suspend fun getItemsForPurchaseOrder(orderId: String): List<PurchaseOrderItemEntity>

    @Query("UPDATE purchase_orders SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateOrderStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE purchase_order_items SET receivedQuantity = :receivedQuantity WHERE id = :itemId")
    suspend fun updateItemReceivedQuantity(itemId: String, receivedQuantity: Double)

    @Transaction
    suspend fun saveFullPurchaseOrder(order: PurchaseOrderEntity, items: List<PurchaseOrderItemEntity>) {
        insertPurchaseOrder(order)
        insertPurchaseOrderItems(items)
    }

    @Query("SELECT COUNT(*) FROM purchase_orders")
    suspend fun getPurchaseOrderCount(): Int
}
