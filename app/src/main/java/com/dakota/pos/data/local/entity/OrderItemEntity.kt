package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId")]
)
data class OrderItemEntity(
    @PrimaryKey
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
