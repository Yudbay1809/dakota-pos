package com.dakota.pos.domain.usecase.payment

import com.dakota.pos.domain.model.Order
import com.dakota.pos.domain.model.OrderStatus
import com.dakota.pos.domain.payment.PaymentMethodType
import com.dakota.pos.domain.payment.PaymentProcessorRegistry
import com.dakota.pos.domain.payment.PaymentRequest
import com.dakota.pos.domain.payment.PaymentResult
import com.dakota.pos.domain.repository.OrderRepository

class ProcessPaymentUseCase(
    private val paymentProcessorRegistry: PaymentProcessorRegistry,
    private val orderRepository: OrderRepository
) {

    sealed class Result {
        data class Success(
            val order: Order,
            val paymentResult: PaymentResult.Success
        ) : Result()

        data class Error(val message: String) : Result()
    }

    suspend operator fun invoke(
        orderId: String,
        method: PaymentMethodType,
        cashReceived: Double? = null,
        referenceNumber: String? = null
    ): Result {
        val order = orderRepository.getOrderById(orderId)
            ?: return Result.Error("Pesanan dengan ID $orderId tidak ditemukan.")

        if (order.status == OrderStatus.PAID || order.status == OrderStatus.COMPLETED) {
            return Result.Error("Pesanan ini sudah dibayar sebelumnya.")
        }

        if (order.status == OrderStatus.CANCELLED) {
            return Result.Error("Pesanan telah dibatalkan.")
        }

        val paymentRequest = PaymentRequest(
            orderId = orderId,
            totalAmount = order.total,
            method = method,
            cashReceived = cashReceived,
            referenceNumber = referenceNumber
        )

        return when (val paymentResult = paymentProcessorRegistry.process(paymentRequest)) {
            is PaymentResult.Success -> {
                // Perbarui status pesanan menjadi PAID dengan metode pembayarannya
                val isUpdated = orderRepository.markOrderAsPaid(
                    orderId = orderId,
                    paymentMethod = method.name
                )

                if (isUpdated) {
                    val updatedOrder = order.copy(
                        status = OrderStatus.PAID,
                        paymentMethod = method.name
                    )
                    Result.Success(updatedOrder, paymentResult)
                } else {
                    Result.Error("Gagal memperbarui status pembayaran di database.")
                }
            }
            is PaymentResult.Failure -> {
                Result.Error(paymentResult.errorMessage)
            }
        }
    }
}
