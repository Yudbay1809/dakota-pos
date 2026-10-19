package com.dakota.pos.domain.model

import java.util.UUID

data class CartItem(
    val id: String = UUID.randomUUID().toString(),
    val product: Product,
    val selectedVariant: ProductVariant? = null,
    val selectedModifiers: List<Modifier> = emptyList(),
    val quantity: Int = 1,
    val notes: String? = null
) {
    val unitPrice: Double
        get() {
            val variantAdjust = selectedVariant?.priceAdjustment ?: 0.0
            val modifierTotal = selectedModifiers.sumOf { it.price }
            return product.basePrice + variantAdjust + modifierTotal
        }

    val subtotal: Double
        get() = unitPrice * quantity

    val summaryDescription: String
        get() {
            val parts = mutableListOf<String>()
            selectedVariant?.let { parts.add(it.name) }
            if (selectedModifiers.isNotEmpty()) {
                parts.add(selectedModifiers.joinToString(", ") { it.name })
            }
            notes?.takeIf { it.isNotBlank() }?.let { parts.add("Catatan: $it") }
            return parts.joinToString(" • ")
        }
}

data class Cart(
    val items: List<CartItem> = emptyList(),
    val discount: Double = 0.0,
    val customerNotes: String? = null
) {
    val totalItemCount: Int
        get() = items.sumOf { it.quantity }

    val subtotal: Double
        get() = items.sumOf { it.subtotal }

    val total: Double
        get() = maxOf(0.0, subtotal - discount)
}
