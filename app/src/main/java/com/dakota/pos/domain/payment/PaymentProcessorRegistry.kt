package com.dakota.pos.domain.payment

/**
 * Registry penyedia processor pembayaran multichannel.
 * Mengisolasi business logic POS dari implementasi spesifik masing-masing gateway/metode pembayaran.
 */
class PaymentProcessorRegistry(
    val cashProcessor: CashPaymentProcessor = CashPaymentProcessor(),
    val qrisProcessor: MockQrisPaymentProcessor = MockQrisPaymentProcessor(),
    private val cardProcessors: Map<PaymentMethodType, PaymentProcessor> = mapOf(
        PaymentMethodType.DEBIT_CARD to GenericCardPaymentProcessor(PaymentMethodType.DEBIT_CARD),
        PaymentMethodType.CREDIT_CARD to GenericCardPaymentProcessor(PaymentMethodType.CREDIT_CARD),
        PaymentMethodType.BANK_TRANSFER to GenericCardPaymentProcessor(PaymentMethodType.BANK_TRANSFER),
        PaymentMethodType.E_WALLET to GenericCardPaymentProcessor(PaymentMethodType.E_WALLET)
    )
) {

    fun getProcessor(method: PaymentMethodType): PaymentProcessor {
        return when (method) {
            PaymentMethodType.CASH -> cashProcessor
            PaymentMethodType.QRIS -> qrisProcessor
            else -> cardProcessors[method]
                ?: throw IllegalArgumentException("Tidak ada processor yang terdaftar untuk metode: $method")
        }
    }

    suspend fun process(request: PaymentRequest): PaymentResult {
        val processor = getProcessor(request.method)
        return processor.processPayment(request)
    }
}
