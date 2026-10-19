package com.dakota.pos.domain.repository

import com.dakota.pos.domain.model.Cart
import com.dakota.pos.domain.model.Order
import com.dakota.pos.domain.model.OrderStatus
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    suspend fun createOrderFromCart(
        cart: Cart,
        cashierId: String,
        cashierName: String,
        initialStatus: OrderStatus = OrderStatus.CONFIRMED
    ): Order

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Boolean
    suspend fun markOrderAsPaid(orderId: String, paymentMethod: String): Boolean
    suspend fun getOrderById(orderId: String): Order?
    fun getRecentOrders(): Flow<List<Order>>
    suspend fun getPaidOrdersBetween(startTime: Long, endTime: Long): List<Order>
}
