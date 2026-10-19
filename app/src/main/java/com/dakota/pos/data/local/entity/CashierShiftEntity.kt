package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cashier_shifts")
data class CashierShiftEntity(
    @PrimaryKey
    val id: String,
    val cashierId: String,
    val cashierName: String,
    val startTime: Long,
    val endTime: Long?,
    val status: String, // OPEN, CLOSED
    val openingCash: Double,
    val cashSales: Double,
    val nonCashSales: Double,
    val totalCashIn: Double,
    val totalCashOut: Double,
    val totalExpense: Double,
    val totalRefund: Double,
    val expectedCash: Double,
    val actualCash: Double?,
    val variance: Double?,
    val closingNotes: String?,
    val totalTransactions: Int,
    val createdAt: Long = System.currentTimeMillis()
)
