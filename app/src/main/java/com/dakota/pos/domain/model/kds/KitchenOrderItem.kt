package com.dakota.pos.domain.model.kds

data class KitchenOrderItem(
    val id: String,
    val kitchenOrderId: String,
    val orderItemId: String,
    val productId: String,
    val productName: String,
    val variantName: String? = null,
    val modifiersSummary: String? = null,
    val quantity: Int,
    val notes: String? = null
)
