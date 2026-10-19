package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kitchen_orders")
data class KitchenOrderEntity(
    @PrimaryKey
    val id: String,
    val orderId: String,
    val orderNumber: String,
    val stationId: String, // station_bar, station_kitchen, station_dessert
    val status: String,    // NEW, ACCEPTED, PREPARING, READY, SERVED, CANCELLED
    val customerNotes: String?,
    val cashierName: String,
    val orderTime: Long,
    val acceptedTime: Long?,
    val readyTime: Long?,
    val servedTime: Long?,
    val createdAt: Long = System.currentTimeMillis()
)
