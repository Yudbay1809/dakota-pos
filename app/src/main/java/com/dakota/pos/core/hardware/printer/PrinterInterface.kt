package com.dakota.pos.core.hardware.printer

enum class PrinterType {
    BLUETOOTH_ESC_POS,
    NETWORK_LAN_ESC_POS,
    USB_ESC_POS,
    MOCK
}

enum class PrinterStatus {
    CONNECTED,
    DISCONNECTED,
    OUT_OF_PAPER,
    COVER_OPEN,
    ERROR
}

data class PrintReceiptItem(
    val name: String,
    val variantModifier: String? = null,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double
)

data class PrintReceiptData(
    val outletName: String,
    val outletAddress: String? = null,
    val orderNumber: String,
    val dateTime: String,
    val cashierName: String,
    val items: List<PrintReceiptItem>,
    val subtotal: Double,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double,
    val paymentMethod: String,
    val cashReceived: Double? = null,
    val change: Double? = null,
    val referenceNumber: String? = null,
    val footerMessage: String? = "Terima kasih atas kunjungan Anda!",
    val footerSocial: String? = "IG: @dakotacoffee.id"
)

sealed class PrintResult {
    data class Success(val message: String = "Print completed") : PrintResult()
    data class Failure(val error: String, val canRetry: Boolean = true) : PrintResult()
}

/**
 * Interface abstraksi printer agar business logic POS tidak terikat langsung
 * dengan driver perangkat keras tertentu.
 */
interface PrinterInterface {
    val printerType: PrinterType
    suspend fun connect(targetAddress: String): Result<Boolean>
    suspend fun disconnect()
    suspend fun checkStatus(): PrinterStatus
    suspend fun printReceipt(receipt: PrintReceiptData): PrintResult
    suspend fun printBaristaTicket(orderNumber: String, stationName: String, items: List<PrintReceiptItem>): PrintResult
    suspend fun printRawBytes(data: ByteArray): Boolean = false
    suspend fun cutPaper(): Boolean = false
    suspend fun openCashDrawer(): Boolean
}
