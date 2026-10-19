package com.dakota.pos.core.hardware.printer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Adapter Printer ESC/POS berbasis Jaringan LAN / WiFi (TCP/IP Raw Socket).
 * Standar printer thermal menggunakan port 9100.
 */
class NetworkEscPosPrinterAdapter(
    val host: String = "192.168.1.200",
    val port: Int = 9100,
    val timeoutMs: Int = 3000
) : PrinterInterface {

    override val printerType: PrinterType = PrinterType.NETWORK_LAN_ESC_POS

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var isConnected: Boolean = false

    override suspend fun connect(targetAddress: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val (targetHost, targetPort) = parseAddress(targetAddress)
            socket = Socket()
            socket?.connect(InetSocketAddress(targetHost, targetPort), timeoutMs)
            socket?.soTimeout = timeoutMs
            outputStream = socket?.getOutputStream()
            isConnected = true
            Result.success(true)
        } catch (e: Exception) {
            disconnect()
            Result.failure(e)
        }
    }

    private fun parseAddress(address: String): Pair<String, Int> {
        if (address.isBlank()) return host to port
        val parts = address.removePrefix("TCP:").split(":")
        val h = parts.getOrNull(0)?.trim()?.takeIf { it.isNotBlank() } ?: host
        val p = parts.getOrNull(1)?.toIntOrNull() ?: port
        return h to p
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                outputStream?.close()
                socket?.close()
            } catch (_: Exception) {
            } finally {
                outputStream = null
                socket = null
                isConnected = false
            }
        }
    }

    override suspend fun printRawBytes(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            outputStream?.write(data)
            outputStream?.flush()
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun cutPaper(): Boolean = withContext(Dispatchers.IO) {
        try {
            outputStream?.write(EscPosReceiptGenerator.CMD_CUT_PAPER)
            outputStream?.flush()
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun checkStatus(): PrinterStatus = withContext(Dispatchers.IO) {
        if (isConnected && socket?.isConnected == true && socket?.isClosed == false) {
            PrinterStatus.CONNECTED
        } else {
            PrinterStatus.DISCONNECTED
        }
    }

    override suspend fun printReceipt(receipt: PrintReceiptData): PrintResult = withContext(Dispatchers.IO) {
        try {
            if (!isConnected) {
                val conn = connect("$host:$port")
                if (conn.isFailure) return@withContext PrintResult.Failure("Gagal terhubung ke printer jaringan $host:$port")
            }
            val bytes = EscPosReceiptGenerator.generateBytes(receipt)
            outputStream?.write(bytes)
            outputStream?.flush()
            PrintResult.Success("Receipt printed via Network ($host:$port)")
        } catch (e: Exception) {
            PrintResult.Failure("Error printing via Network: ${e.message}")
        }
    }

    override suspend fun printBaristaTicket(
        orderNumber: String,
        stationName: String,
        items: List<PrintReceiptItem>
    ): PrintResult = withContext(Dispatchers.IO) {
        try {
            if (!isConnected) {
                val conn = connect("$host:$port")
                if (conn.isFailure) return@withContext PrintResult.Failure("Gagal terhubung ke printer jaringan $host:$port")
            }
            outputStream?.write(EscPosReceiptGenerator.CMD_INIT)
            outputStream?.write(EscPosReceiptGenerator.CMD_ALIGN_CENTER)
            outputStream?.write(EscPosReceiptGenerator.CMD_BOLD_ON)
            outputStream?.write(EscPosReceiptGenerator.CMD_FONT_DOUBLE_SIZE)
            outputStream?.write("*** ${stationName.uppercase()} ***\n".toByteArray())
            outputStream?.write(EscPosReceiptGenerator.CMD_FONT_NORMAL)
            outputStream?.write(EscPosReceiptGenerator.CMD_BOLD_OFF)
            outputStream?.write("ORDER #$orderNumber\n--------------------------------\n".toByteArray())
            outputStream?.write(EscPosReceiptGenerator.CMD_ALIGN_LEFT)
            items.forEach { item ->
                outputStream?.write("${item.quantity}x ${item.name}\n".toByteArray())
                if (!item.variantModifier.isNullOrBlank()) {
                    outputStream?.write("  (${item.variantModifier})\n".toByteArray())
                }
            }
            outputStream?.write("--------------------------------\n\n\n".toByteArray())
            outputStream?.write(EscPosReceiptGenerator.CMD_CUT_PAPER)
            outputStream?.flush()
            PrintResult.Success("Barista ticket printed via Network")
        } catch (e: Exception) {
            PrintResult.Failure("Error printing ticket via Network: ${e.message}")
        }
    }

    override suspend fun openCashDrawer(): Boolean = withContext(Dispatchers.IO) {
        try {
            outputStream?.write(EscPosReceiptGenerator.CMD_OPEN_DRAWER)
            outputStream?.flush()
            true
        } catch (_: Exception) {
            false
        }
    }
}
