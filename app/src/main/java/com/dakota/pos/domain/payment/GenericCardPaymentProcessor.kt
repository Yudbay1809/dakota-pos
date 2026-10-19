package com.dakota.pos.domain.payment

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Processor pembayaran untuk kartu EDC (Debit/Credit), transfer bank, dan e-wallet.
 */
class GenericCardPaymentProcessor(
    override val supportedMethod: PaymentMethodType
) : PaymentProcessor {

    override suspend fun processPayment(request: PaymentRequest): PaymentResult {
        if (request.totalAmount <= 0) {
            return PaymentResult.Failure(
                orderId = request.orderId,
                errorMessage = "Total transaksi tidak valid."
            )
        }

        val prefix = when (supportedMethod) {
            PaymentMethodType.DEBIT_CARD -> "DEBIT"
            PaymentMethodType.CREDIT_CARD -> "CREDIT"
            PaymentMethodType.BANK_TRANSFER -> "TRF"
            PaymentMethodType.E_WALLET -> "EWL"
            else -> "CARD"
        }

        val dateCode = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val randomSuffix = UUID.randomUUID().toString().take(6).uppercase()
        val refNumber = request.referenceNumber.takeIf { !it.isNullOrBlank() }
            ?: "$prefix-$dateCode-$randomSuffix"

        return PaymentResult.Success(
            orderId = request.orderId,
            totalAmount = request.totalAmount,
            method = supportedMethod,
            change = 0.0,
            transactionReference = refNumber
        )
    }

    override suspend fun cancelPayment(orderId: String): Boolean = true
}
