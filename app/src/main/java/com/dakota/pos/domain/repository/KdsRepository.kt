package com.dakota.pos.domain.repository

import com.dakota.pos.domain.model.kds.KitchenOrder
import com.dakota.pos.domain.model.kds.KitchenOrderStatus
import com.dakota.pos.domain.model.kds.StationConfig
import com.dakota.pos.domain.model.kds.WorkStation
import kotlinx.coroutines.flow.Flow

interface KdsRepository {

    suspend fun saveKitchenOrder(order: KitchenOrder)
    suspend fun getKitchenOrderById(id: String): KitchenOrder?
    suspend fun updateOrderStatus(id: String, status: KitchenOrderStatus)
    fun getActiveOrdersFlow(): Flow<List<KitchenOrder>>
    fun getActiveOrdersByStationFlow(station: WorkStation): Flow<List<KitchenOrder>>
    fun getAllOrdersFlow(): Flow<List<KitchenOrder>>

    suspend fun getAllStationConfigs(): List<StationConfig>
    fun getStationConfigsFlow(): Flow<List<StationConfig>>
    suspend fun getStationConfig(station: WorkStation): StationConfig?
    suspend fun saveStationConfig(config: StationConfig)
    suspend fun seedDefaultStationConfigsIfEmpty()
}
