package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dakota.pos.domain.model.inventory.MovementType

@Entity(
    tableName = "stock_movements",
    indices = [
        Index(value = ["ingredientId"]),
        Index(value = ["referenceId"]),
        Index(value = ["type"]),
        Index(value = ["timestamp"])
    ]
)
data class StockMovementEntity(
    @PrimaryKey
    val id: String,
    val ingredientId: String,
    val ingredientName: String,
    val type: MovementType,
    val quantityChange: Double,
    val unit: String,
    val balanceBefore: Double,
    val balanceAfter: Double,
    val referenceId: String? = null,
    val notes: String? = null,
    val userId: String,
    val userName: String,
    val timestamp: Long = System.currentTimeMillis()
)
