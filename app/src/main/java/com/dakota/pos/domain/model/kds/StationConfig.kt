package com.dakota.pos.domain.model.kds

import com.dakota.pos.core.hardware.printer.PrinterType

data class StationConfig(
    val station: WorkStation,
    val name: String,
    val printerType: PrinterType = PrinterType.MOCK,
    val targetAddress: String = "MOCK",
    val autoPrintOnDispatch: Boolean = true,
    val categoryIds: List<String> = emptyList() // Category IDs routed to this station
)
