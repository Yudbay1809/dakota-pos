package com.dakota.pos.domain.model.purchasing

import com.dakota.pos.domain.model.unit.UnitType

data class GoodsReceiptItem(
    val id: String,
    val goodsReceiptId: String,
    val purchaseOrderItemId: String,
    val ingredientId: String,
    val ingredientName: String,
    val unit: UnitType,
    val receivedQuantity: Double,
    val unitPrice: Double,
    val notes: String? = null
)

data class GoodsReceipt(
    val id: String,
    val receiptNumber: String,
    val purchaseOrderId: String,
    val poNumber: String,
    val supplierName: String,
    val receivedDate: Long = System.currentTimeMillis(),
    val receivedBy: String,
    val notes: String? = null,
    val items: List<GoodsReceiptItem> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
