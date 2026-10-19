package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchase_orders")
data class PurchaseOrderEntity(
    @PrimaryKey
    val id: String,
    val poNumber: String,
    val supplierId: String,
    val supplierName: String,
    val orderDate: Long,
    val expectedDeliveryDate: Long?,
    val status: String, // DRAFT, SUBMITTED, RECEIVED, COMPLETED, CANCELLED
    val totalAmount: Double,
    val notes: String?,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
