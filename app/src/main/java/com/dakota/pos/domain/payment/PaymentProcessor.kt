package com.dakota.pos.domain.payment

enum class PaymentMethodType {
    CASH,
    QRIS,
    DEBIT_CARD,
    CREDIT_CARD,
    BANK_TRANSFER,
    E_WALLET
}

data class PaymentRequest(
    val orderId: String,
    val totalAmount: Double,
    val method: PaymentMethodType,
    val cashReceived: Double? = null,
    val referenceNumber: String? = null
)

sealed class PaymentResult {
    data class Success(
        val orderId: String,
        val totalAmount: Double,
        val method: PaymentMethodType,
        val change: Double = 0.0,
        val transactionReference: String? = null
    ) : PaymentResult()

    data class Failure(
        val orderId: String,
        val errorMessage: String
    ) : PaymentResult()
}

/**
 * Abstraksi prosesor pembayaran agar penambahan penyedia QRIS (misal: Midtrans, Xendit, QRIS Dinamis Lokal)
 * maupun pembayaran Cash & EDC tidak merusak logika POS inti.
 */
interface PaymentProcessor {
    val supportedMethod: PaymentMethodType
    suspend fun processPayment(request: PaymentRequest): PaymentResult
    suspend fun cancelPayment(orderId: String): Boolean
}
