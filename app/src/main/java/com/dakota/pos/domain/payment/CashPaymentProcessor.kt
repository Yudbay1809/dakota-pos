package com.dakota.pos.domain.payment

/**
 * Processor pembayaran tunai (Cash) dengan validasi uang diterima dan kalkulasi kembalian.
 */
class CashPaymentProcessor : PaymentProcessor {

    override val supportedMethod: PaymentMethodType = PaymentMethodType.CASH

    override suspend fun processPayment(request: PaymentRequest): PaymentResult {
        val cashReceived = request.cashReceived
            ?: return PaymentResult.Failure(
                orderId = request.orderId,
                errorMessage = "Nominal uang yang diterima wajib diisi."
            )

        if (cashReceived < request.totalAmount) {
            val shortage = request.totalAmount - cashReceived
            return PaymentResult.Failure(
                orderId = request.orderId,
                errorMessage = "Nominal uang kurang Rp %,.0f dari total tagihan.".format(shortage)
            )
        }

        val change = cashReceived - request.totalAmount

        return PaymentResult.Success(
            orderId = request.orderId,
            totalAmount = request.totalAmount,
            method = PaymentMethodType.CASH,
            change = change,
            transactionReference = "CASH-${System.currentTimeMillis()}"
        )
    }

    override suspend fun cancelPayment(orderId: String): Boolean {
        // Pembayaran tunai dapat dibatalkan di POS sebelum shift ditutup
        return true
    }
}
