package com.dakota.pos.core.hardware.printer

/**
 * Implementasi Mock Printer untuk pengujian unit, instrumented test,
 * dan preview struk tanpa printer fisik.
 */
class MockPrinter : PrinterInterface {
    override val printerType: PrinterType = PrinterType.MOCK

    var isConnected: Boolean = true
    var lastPrintedReceipt: PrintReceiptData? = null
    var lastPrintedBaristaTicket: String? = null
    var cashDrawerOpened: Boolean = false

    override suspend fun connect(targetAddress: String): Result<Boolean> {
        isConnected = true
        return Result.success(true)
    }

    override suspend fun disconnect() {
        isConnected = false
    }

    override suspend fun checkStatus(): PrinterStatus {
        return if (isConnected) PrinterStatus.CONNECTED else PrinterStatus.DISCONNECTED
    }

    override suspend fun printReceipt(receipt: PrintReceiptData): PrintResult {
        return if (isConnected) {
            lastPrintedReceipt = receipt
            PrintResult.Success("Receipt printed successfully (Mock)")
        } else {
            PrintResult.Failure("Printer is disconnected", canRetry = true)
        }
    }

    override suspend fun printBaristaTicket(
        orderNumber: String,
        stationName: String,
        items: List<PrintReceiptItem>
    ): PrintResult {
        return if (isConnected) {
            lastPrintedBaristaTicket = "$orderNumber - $stationName"
            PrintResult.Success("Barista ticket printed successfully (Mock)")
        } else {
            PrintResult.Failure("Printer is disconnected", canRetry = true)
        }
    }

    override suspend fun printRawBytes(data: ByteArray): Boolean {
        return isConnected
    }

    override suspend fun cutPaper(): Boolean {
        return isConnected
    }

    override suspend fun openCashDrawer(): Boolean {
        cashDrawerOpened = true
        return isConnected
    }
}
