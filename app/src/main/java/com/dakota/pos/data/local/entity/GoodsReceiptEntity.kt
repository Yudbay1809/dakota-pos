package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goods_receipts")
data class GoodsReceiptEntity(
    @PrimaryKey
    val id: String,
    val receiptNumber: String,
    val purchaseOrderId: String,
    val poNumber: String,
    val supplierName: String,
    val receivedDate: Long,
    val receivedBy: String,
    val notes: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "goods_receipt_items")
data class GoodsReceiptItemEntity(
    @PrimaryKey
    val id: String,
    val goodsReceiptId: String,
    val purchaseOrderItemId: String,
    val ingredientId: String,
    val ingredientName: String,
    val unit: String,
    val receivedQuantity: Double,
    val unitPrice: Double,
    val notes: String?
)
