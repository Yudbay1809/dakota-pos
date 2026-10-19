package com.dakota.pos.domain.payment

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Abstraksi & Mock Provider untuk QRIS Dinamis.
 * Dapat diganti dengan provider riil (Midtrans, Xendit, BCA, dll.) tanpa mengubah domain logic POS.
 */
class MockQrisPaymentProcessor(
    private val merchantName: String = "Dakota Coffee Shop",
    private val nmid: String = "ID1020000000001"
) : PaymentProcessor {

    override val supportedMethod: PaymentMethodType = PaymentMethodType.QRIS

    /**
     * Menghasilkan representasi string payload QRIS dinamis untuk dirender sebagai QR code.
     */
    fun generateDynamicQrisPayload(orderId: String, amount: Double): String {
        // Format mock payload standar QRIS EMVCo
        return "00020101021226${nmid.length}${nmid}52045812530336054${String.format(Locale.US, "%02d", amount.toInt().toString().length)}${amount.toInt()}5802ID59${merchantName.length}${merchantName}6007JAKARTA62${orderId.length}${orderId}6304MOCK"
    }

    override suspend fun processPayment(request: PaymentRequest): PaymentResult {
        if (request.totalAmount <= 0) {
            return PaymentResult.Failure(
                orderId = request.orderId,
                errorMessage = "Total tagihan QRIS tidak valid."
            )
        }

        val dateCode = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val randomSuffix = UUID.randomUUID().toString().take(6).uppercase()
        val refNumber = request.referenceNumber ?: "QRIS-$dateCode-$randomSuffix"

        return PaymentResult.Success(
            orderId = request.orderId,
            totalAmount = request.totalAmount,
            method = PaymentMethodType.QRIS,
            change = 0.0,
            transactionReference = refNumber
        )
    }

    override suspend fun cancelPayment(orderId: String): Boolean {
        return true
    }
}
