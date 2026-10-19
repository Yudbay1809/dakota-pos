package com.dakota.pos.domain.usecase.pos

import com.dakota.pos.domain.model.Cart
import com.dakota.pos.domain.model.Order
import com.dakota.pos.domain.model.OrderStatus
import com.dakota.pos.domain.repository.OrderRepository

/**
 * Converts a finalized Cart into a persisted Order.
 * Order lifecycle: DRAFT → CONFIRMED → PAID → COMPLETED
 *
 * For POS quick-checkout, initial status is CONFIRMED
 * (skips DRAFT since the cashier already confirmed items).
 */
class CheckoutOrderUseCase(
    private val orderRepository: OrderRepository
) {
    sealed class Result {
        data class Success(val order: Order) : Result()
        data class Error(val message: String) : Result()
    }

    suspend operator fun invoke(
        cart: Cart,
        cashierId: String,
        cashierName: String
    ): Result {
        // Validations
        if (cart.items.isEmpty()) {
            return Result.Error("Keranjang kosong. Tambahkan item terlebih dahulu.")
        }

        if (cart.total <= 0) {
            return Result.Error("Total pesanan harus lebih dari Rp 0.")
        }

        return try {
            val order = orderRepository.createOrderFromCart(
                cart = cart,
                cashierId = cashierId,
                cashierName = cashierName,
                initialStatus = OrderStatus.CONFIRMED
            )
            Result.Success(order)
        } catch (e: Exception) {
            Result.Error("Gagal menyimpan pesanan: ${e.message}")
        }
    }
}
