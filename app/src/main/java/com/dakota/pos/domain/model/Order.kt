package com.dakota.pos.domain.model

data class OrderItem(
    val id: String,
    val orderId: String,
    val productId: String,
    val productName: String,
    val variantId: String? = null,
    val variantName: String? = null,
    val selectedModifiersSummary: String? = null,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double,
    val notes: String? = null
)

data class Order(
    val id: String,
    val orderNumber: String,
    val cashierId: String,
    val cashierName: String,
    val status: OrderStatus = OrderStatus.DRAFT,
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double,
    val customerNotes: String? = null,
    val paymentMethod: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
