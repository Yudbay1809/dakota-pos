package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_opnames")
data class StockOpnameEntity(
    @PrimaryKey
    val id: String,
    val opnameNumber: String,
    val performedByUserId: String,
    val performedByUserName: String,
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
