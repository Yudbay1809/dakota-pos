package com.dakota.pos.domain.usecase.pos

import com.dakota.pos.domain.model.Cart
import com.dakota.pos.domain.model.CartItem
import com.dakota.pos.domain.model.Modifier
import com.dakota.pos.domain.model.Product
import com.dakota.pos.domain.model.ProductVariant

class CalculateCartUseCase {

    fun addItem(
        cart: Cart,
        product: Product,
        variant: ProductVariant? = null,
        modifiers: List<Modifier> = emptyList(),
        quantity: Int = 1,
        notes: String? = null
    ): Cart {
        val existingIndex = cart.items.indexOfFirst {
            it.product.id == product.id &&
            it.selectedVariant?.id == variant?.id &&
            it.selectedModifiers.map { m -> m.id }.sorted() == modifiers.map { m -> m.id }.sorted() &&
            it.notes == notes
        }

        val updatedItems = cart.items.toMutableList()
        if (existingIndex != -1) {
            val existing = updatedItems[existingIndex]
            updatedItems[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            updatedItems.add(
                CartItem(
                    product = product,
                    selectedVariant = variant,
                    selectedModifiers = modifiers,
                    quantity = quantity,
                    notes = notes
                )
            )
        }

        return cart.copy(items = updatedItems)
    }

    fun updateQuantity(cart: Cart, itemId: String, delta: Int): Cart {
        val updatedItems = cart.items.mapNotNull {
            if (it.id == itemId) {
                val newQty = it.quantity + delta
                if (newQty > 0) it.copy(quantity = newQty) else null
            } else {
                it
            }
        }
        return cart.copy(items = updatedItems)
    }

    fun removeItem(cart: Cart, itemId: String): Cart {
        return cart.copy(items = cart.items.filterNot { it.id == itemId })
    }

    fun applyDiscount(cart: Cart, discountAmount: Double): Cart {
        val validDiscount = maxOf(0.0, minOf(discountAmount, cart.subtotal))
        return cart.copy(discount = validDiscount)
    }

    fun clearCart(): Cart {
        return Cart()
    }
}
