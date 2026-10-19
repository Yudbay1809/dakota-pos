package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dakota.pos.domain.model.OrderStatus

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey
    val id: String,
    val orderNumber: String, // e.g. "DKT-20261014-001"
    val cashierId: String,
    val cashierName: String,
    val status: OrderStatus = OrderStatus.DRAFT,
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double = 0.0,
    val customerNotes: String? = null,
    val paymentMethod: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
