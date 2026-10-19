package com.dakota.pos.data.repository

import com.dakota.pos.data.local.dao.OrderDao
import com.dakota.pos.data.local.entity.OrderEntity
import com.dakota.pos.data.local.entity.OrderItemEntity
import com.dakota.pos.domain.model.Cart
import com.dakota.pos.domain.model.Order
import com.dakota.pos.domain.model.OrderItem
import com.dakota.pos.domain.model.OrderStatus
import com.dakota.pos.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class OrderRepositoryImpl(
    private val orderDao: OrderDao
) : OrderRepository {

    override suspend fun createOrderFromCart(
        cart: Cart,
        cashierId: String,
        cashierName: String,
        initialStatus: OrderStatus
    ): Order {
        val orderId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val orderNumber = generateOrderNumber(now)

        val orderEntity = OrderEntity(
            id = orderId,
            orderNumber = orderNumber,
            cashierId = cashierId,
            cashierName = cashierName,
            status = initialStatus,
            subtotal = cart.subtotal,
            discount = cart.discount,
            tax = 0.0, // Tax calculation will be added in Phase 9
            total = cart.total,
            customerNotes = cart.customerNotes,
            paymentMethod = null,
            createdAt = now,
            updatedAt = now
        )

        val orderItemEntities = cart.items.map { cartItem ->
            OrderItemEntity(
                id = UUID.randomUUID().toString(),
                orderId = orderId,
                productId = cartItem.product.id,
                productName = cartItem.product.name,
                variantId = cartItem.selectedVariant?.id,
                variantName = cartItem.selectedVariant?.name,
                selectedModifiersSummary = if (cartItem.selectedModifiers.isNotEmpty()) {
                    cartItem.selectedModifiers.joinToString(", ") { it.name }
                } else null,
                quantity = cartItem.quantity,
                unitPrice = cartItem.unitPrice,
                subtotal = cartItem.subtotal,
                notes = cartItem.notes
            )
        }

        orderDao.insertOrder(orderEntity)
        orderDao.insertOrderItems(orderItemEntities)

        return Order(
            id = orderId,
            orderNumber = orderNumber,
            cashierId = cashierId,
            cashierName = cashierName,
            status = initialStatus,
            items = orderItemEntities.map { it.toDomain() },
            subtotal = cart.subtotal,
            discount = cart.discount,
            tax = 0.0,
            total = cart.total,
            customerNotes = cart.customerNotes,
            createdAt = now,
            updatedAt = now
        )
    }

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Boolean {
        return try {
            orderDao.updateOrderStatus(orderId, status)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun markOrderAsPaid(orderId: String, paymentMethod: String): Boolean {
        return try {
            orderDao.updateOrderPayment(orderId, OrderStatus.PAID, paymentMethod)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getOrderById(orderId: String): Order? {
        val entity = orderDao.getOrderById(orderId) ?: return null
        val items = orderDao.getItemsForOrder(orderId)
        return entity.toDomain(items.map { it.toDomain() })
    }

    override fun getRecentOrders(): Flow<List<Order>> {
        return orderDao.getRecentOrders().map { entities ->
            entities.map { entity ->
                val items = orderDao.getItemsForOrder(entity.id)
                entity.toDomain(items.map { it.toDomain() })
            }
        }
    }

    override suspend fun getPaidOrdersBetween(startTime: Long, endTime: Long): List<Order> {
        val entities = orderDao.getPaidOrdersBetween(startTime, endTime)
        return entities.map { entity ->
            val items = orderDao.getItemsForOrder(entity.id)
            entity.toDomain(items.map { it.toDomain() })
        }
    }

    // --- Helpers ---

    private suspend fun generateOrderNumber(timestamp: Long): String {
        val datePart = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(timestamp))
        val count = orderDao.getOrderCount() + 1
        return "DKT-$datePart-%03d".format(count)
    }

    // --- Mappers ---

    private fun OrderEntity.toDomain(items: List<OrderItem>) = Order(
        id = id,
        orderNumber = orderNumber,
        cashierId = cashierId,
        cashierName = cashierName,
        status = status,
        items = items,
        subtotal = subtotal,
        discount = discount,
        tax = tax,
        total = total,
        customerNotes = customerNotes,
        paymentMethod = paymentMethod,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun OrderItemEntity.toDomain() = OrderItem(
        id = id,
        orderId = orderId,
        productId = productId,
        productName = productName,
        variantId = variantId,
        variantName = variantName,
        selectedModifiersSummary = selectedModifiersSummary,
        quantity = quantity,
        unitPrice = unitPrice,
        subtotal = subtotal,
        notes = notes
    )
}
