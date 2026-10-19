package com.dakota.pos.core.hardware.printer

import com.dakota.pos.domain.model.kds.StationConfig
import com.dakota.pos.domain.model.kds.WorkStation
import java.util.concurrent.ConcurrentHashMap

class PrinterManager {

    private val stationPrinters = ConcurrentHashMap<WorkStation, PrinterInterface>()

    init {
        // Default ke MockPrinter agar 100% aman diuji tanpa printer fisik
        WorkStation.entries.forEach { station ->
            stationPrinters[station] = MockPrinter()
        }
    }

    fun getPrinter(station: WorkStation): PrinterInterface {
        return stationPrinters.getOrPut(station) { MockPrinter() }
    }

    fun configurePrinter(config: StationConfig) {
        val adapter: PrinterInterface = when (config.printerType) {
            PrinterType.MOCK -> MockPrinter()
            PrinterType.BLUETOOTH_ESC_POS -> BluetoothEscPosPrinterAdapter(macAddress = config.targetAddress)
            PrinterType.NETWORK_LAN_ESC_POS -> {
                val parts = config.targetAddress.removePrefix("TCP:").split(":")
                val host = parts.getOrNull(0) ?: "192.168.1.200"
                val port = parts.getOrNull(1)?.toIntOrNull() ?: 9100
                NetworkEscPosPrinterAdapter(host = host, port = port)
            }
            PrinterType.USB_ESC_POS -> MockPrinter()
        }
        stationPrinters[config.station] = adapter
    }

    fun setPrinterForStation(station: WorkStation, printer: PrinterInterface) {
        stationPrinters[station] = printer
    }
}
