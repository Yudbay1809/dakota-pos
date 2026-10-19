package com.dakota.pos.core.hardware.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

/**
 * Adapter Printer ESC/POS berbasis Bluetooth SPP (Serial Port Profile).
 * Menggunakan standard SPP UUID: 00001101-0000-1000-8000-00805F9B34FB.
 */
class BluetoothEscPosPrinterAdapter(
    val macAddress: String? = null
) : PrinterInterface {

    override val printerType: PrinterType = PrinterType.BLUETOOTH_ESC_POS

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var isConnected: Boolean = false

    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override suspend fun connect(targetAddress: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val address = targetAddress.ifBlank { macAddress ?: return@withContext Result.failure(IllegalArgumentException("MAC Address Bluetooth belum ditentukan")) }
            val adapter = BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext Result.failure(IllegalStateException("Perangkat ini tidak mendukung Bluetooth"))

            if (!adapter.isEnabled) {
                return@withContext Result.failure(IllegalStateException("Bluetooth sedang nonaktif"))
            }

            val device: BluetoothDevice = adapter.getRemoteDevice(address)
            adapter.cancelDiscovery()

            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket?.connect()
            outputStream = socket?.outputStream
            isConnected = true
            Result.success(true)
        } catch (e: Exception) {
            disconnect()
            Result.failure(e)
        }
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
        if (isConnected && socket?.isConnected == true) {
            PrinterStatus.CONNECTED
        } else {
            PrinterStatus.DISCONNECTED
        }
    }

    override suspend fun printReceipt(receipt: PrintReceiptData): PrintResult = withContext(Dispatchers.IO) {
        try {
            if (!isConnected) {
                val conn = connect(macAddress ?: "")
                if (conn.isFailure) return@withContext PrintResult.Failure("Gagal terhubung ke printer Bluetooth")
            }
            val bytes = EscPosReceiptGenerator.generateBytes(receipt)
            outputStream?.write(bytes)
            outputStream?.flush()
            PrintResult.Success("Receipt printed via Bluetooth")
        } catch (e: Exception) {
            PrintResult.Failure("Error printing via Bluetooth: ${e.message}")
        }
    }

    override suspend fun printBaristaTicket(
        orderNumber: String,
        stationName: String,
        items: List<PrintReceiptItem>
    ): PrintResult = withContext(Dispatchers.IO) {
        try {
            if (!isConnected) {
                val conn = connect(macAddress ?: "")
                if (conn.isFailure) return@withContext PrintResult.Failure("Gagal terhubung ke printer Bluetooth")
            }
            // Kirim tiket stasiun
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
            PrintResult.Success("Barista ticket printed via Bluetooth")
        } catch (e: Exception) {
            PrintResult.Failure("Error printing ticket via Bluetooth: ${e.message}")
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
