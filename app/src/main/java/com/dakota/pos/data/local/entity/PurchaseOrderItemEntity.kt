package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchase_order_items")
data class PurchaseOrderItemEntity(
    @PrimaryKey
    val id: String,
    val purchaseOrderId: String,
    val ingredientId: String,
    val ingredientName: String,
    val unit: String,
    val orderedQuantity: Double,
    val unitPrice: Double,
    val subtotal: Double,
    val receivedQuantity: Double = 0.0
)
