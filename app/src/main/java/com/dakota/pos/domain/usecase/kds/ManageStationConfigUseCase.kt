package com.dakota.pos.domain.usecase.kds

import com.dakota.pos.core.hardware.printer.PrinterManager
import com.dakota.pos.domain.model.kds.StationConfig
import com.dakota.pos.domain.model.kds.WorkStation
import com.dakota.pos.domain.repository.KdsRepository
import kotlinx.coroutines.flow.Flow

class ManageStationConfigUseCase(
    private val kdsRepository: KdsRepository,
    private val printerManager: PrinterManager
) {
    fun getConfigsFlow(): Flow<List<StationConfig>> {
        return kdsRepository.getStationConfigsFlow()
    }

    suspend fun getStationConfig(station: WorkStation): StationConfig? {
        return kdsRepository.getStationConfig(station)
    }

    suspend fun saveConfig(config: StationConfig) {
        kdsRepository.saveStationConfig(config)
        printerManager.configurePrinter(config)
    }

    suspend fun initDefaults() {
        kdsRepository.seedDefaultStationConfigsIfEmpty()
        val configs = kdsRepository.getAllStationConfigs()
        configs.forEach { printerManager.configurePrinter(it) }
    }
}
