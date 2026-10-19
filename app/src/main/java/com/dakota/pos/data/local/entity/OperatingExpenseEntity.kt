package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operating_expenses")
data class OperatingExpenseEntity(
    @PrimaryKey
    val id: String,
    val category: String,
    val amount: Double,
    val description: String,
    val date: Long,
    val recordedBy: String,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
