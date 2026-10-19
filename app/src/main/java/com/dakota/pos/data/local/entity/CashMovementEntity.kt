package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cash_movements")
data class CashMovementEntity(
    @PrimaryKey
    val id: String,
    val shiftId: String,
    val type: String, // CASH_IN, CASH_OUT, EXPENSE, REFUND
    val amount: Double,
    val reason: String,
    val cashierId: String,
    val cashierName: String,
    val timestamp: Long = System.currentTimeMillis()
)
