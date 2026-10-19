package com.dakota.pos.domain.model.purchasing

data class PurchaseOrder(
    val id: String,
    val poNumber: String,
    val supplierId: String,
    val supplierName: String,
    val orderDate: Long = System.currentTimeMillis(),
    val expectedDeliveryDate: Long? = null,
    val status: PurchaseOrderStatus = PurchaseOrderStatus.DRAFT,
    val totalAmount: Double = 0.0,
    val notes: String? = null,
    val items: List<PurchaseOrderItem> = emptyList(),
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
