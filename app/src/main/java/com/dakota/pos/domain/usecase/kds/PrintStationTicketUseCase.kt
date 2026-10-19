package com.dakota.pos.domain.usecase.kds

import com.dakota.pos.core.hardware.printer.PrinterManager
import com.dakota.pos.core.hardware.printer.PrinterStatus
import com.dakota.pos.core.hardware.printer.StationTicketGenerator
import com.dakota.pos.domain.model.kds.KitchenOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrintStationTicketUseCase(
    private val printerManager: PrinterManager
) {
    suspend operator fun invoke(order: KitchenOrder): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val printer = printerManager.getPrinter(order.station)
            val status = printer.checkStatus()
            val isConnected = if (status != PrinterStatus.CONNECTED) {
                printer.connect(order.station.defaultTarget).getOrDefault(false)
            } else {
                true
            }

            if (!isConnected) {
                throw IllegalStateException("Gagal terhubung ke printer stasiun ${order.station.displayName}")
            }

            val ticketBytes = StationTicketGenerator.generateTicketBytes(order)
            val printOk = printer.printRawBytes(ticketBytes)
            if (!printOk) {
                throw IllegalStateException("Gagal mengirim data cetak ke stasiun ${order.station.displayName}")
            }
            printer.cutPaper()
            Unit
        }
    }
}
