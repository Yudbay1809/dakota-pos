package com.dakota.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.dakota.pos.data.local.entity.OrderEntity
import com.dakota.pos.data.local.entity.OrderItemEntity
import com.dakota.pos.domain.model.OrderStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("UPDATE orders SET status = :status, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE orders SET status = :status, paymentMethod = :paymentMethod, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updateOrderPayment(orderId: String, status: OrderStatus, paymentMethod: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getItemsForOrder(orderId: String): List<OrderItemEntity>

    @Query("SELECT * FROM orders ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentOrders(limit: Int = 50): Flow<List<OrderEntity>>

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun getOrderCount(): Int

    @Query("SELECT SUM(total) FROM orders WHERE status = 'PAID' AND paymentMethod = 'CASH' AND createdAt >= :startTime AND createdAt <= :endTime")
    suspend fun getCashSalesForPeriod(startTime: Long, endTime: Long): Double?

    @Query("SELECT SUM(total) FROM orders WHERE status = 'PAID' AND (paymentMethod != 'CASH' OR paymentMethod IS NULL) AND createdAt >= :startTime AND createdAt <= :endTime")
    suspend fun getNonCashSalesForPeriod(startTime: Long, endTime: Long): Double?

    @Query("SELECT COUNT(*) FROM orders WHERE status = 'PAID' AND createdAt >= :startTime AND createdAt <= :endTime")
    suspend fun getTransactionCountForPeriod(startTime: Long, endTime: Long): Int

    @Query("SELECT * FROM orders WHERE status = 'PAID' AND createdAt >= :startTime AND createdAt <= :endTime ORDER BY createdAt ASC")
    suspend fun getPaidOrdersBetween(startTime: Long, endTime: Long): List<OrderEntity>
}
