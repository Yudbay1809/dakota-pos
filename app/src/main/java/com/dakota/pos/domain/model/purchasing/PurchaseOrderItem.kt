package com.dakota.pos.domain.model.purchasing

import com.dakota.pos.domain.model.unit.UnitType

data class PurchaseOrderItem(
    val id: String,
    val purchaseOrderId: String,
    val ingredientId: String,
    val ingredientName: String,
    val unit: UnitType,
    val orderedQuantity: Double,
    val unitPrice: Double,
    val subtotal: Double = orderedQuantity * unitPrice,
    val receivedQuantity: Double = 0.0
) {
    val remainingQuantity: Double
        get() = (orderedQuantity - receivedQuantity).coerceAtLeast(0.0)

    val isFullyReceived: Boolean
        get() = receivedQuantity >= orderedQuantity
}
