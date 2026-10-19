package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kitchen_order_items")
data class KitchenOrderItemEntity(
    @PrimaryKey
    val id: String,
    val kitchenOrderId: String,
    val orderItemId: String,
    val productId: String,
    val productName: String,
    val variantName: String?,
    val modifiersSummary: String?,
    val quantity: Int,
    val notes: String?
)
