package com.dakota.pos.data.repository

import com.dakota.pos.core.hardware.printer.PrinterType
import com.dakota.pos.data.local.dao.KitchenOrderDao
import com.dakota.pos.data.local.dao.StationConfigDao
import com.dakota.pos.data.local.entity.KitchenOrderEntity
import com.dakota.pos.data.local.entity.KitchenOrderItemEntity
import com.dakota.pos.data.local.entity.StationConfigEntity
import com.dakota.pos.domain.model.kds.KitchenOrder
import com.dakota.pos.domain.model.kds.KitchenOrderItem
import com.dakota.pos.domain.model.kds.KitchenOrderStatus
import com.dakota.pos.domain.model.kds.StationConfig
import com.dakota.pos.domain.model.kds.WorkStation
import com.dakota.pos.domain.repository.KdsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class KdsRepositoryImpl(
    private val kitchenOrderDao: KitchenOrderDao,
    private val stationConfigDao: StationConfigDao
) : KdsRepository {

    override suspend fun saveKitchenOrder(order: KitchenOrder) {
        val orderEntity = order.toEntity()
        val itemEntities = order.items.map { it.toEntity(order.id) }
        kitchenOrderDao.saveFullKitchenOrder(orderEntity, itemEntities)
    }

    override suspend fun getKitchenOrderById(id: String): KitchenOrder? {
        val entity = kitchenOrderDao.getKitchenOrderById(id) ?: return null
        val items = kitchenOrderDao.getItemsForKitchenOrder(id).map { it.toDomain() }
        return entity.toDomain(items)
    }

    override suspend fun updateOrderStatus(id: String, status: KitchenOrderStatus) {
        val now = System.currentTimeMillis()
        when (status) {
            KitchenOrderStatus.ACCEPTED -> kitchenOrderDao.markAccepted(id, status.name, now)
            KitchenOrderStatus.READY -> kitchenOrderDao.markReady(id, status.name, now)
            KitchenOrderStatus.SERVED -> kitchenOrderDao.markServed(id, status.name, now)
            else -> kitchenOrderDao.updateStatus(id, status.name)
        }
    }

    override fun getActiveOrdersFlow(): Flow<List<KitchenOrder>> {
        return kitchenOrderDao.getActiveKitchenOrdersFlow().map { entities ->
            entities.map { entity ->
                val items = kitchenOrderDao.getItemsForKitchenOrder(entity.id).map { it.toDomain() }
                entity.toDomain(items)
            }
        }
    }

    override fun getActiveOrdersByStationFlow(station: WorkStation): Flow<List<KitchenOrder>> {
        return kitchenOrderDao.getActiveKitchenOrdersByStationFlow(station.id).map { entities ->
            entities.map { entity ->
                val items = kitchenOrderDao.getItemsForKitchenOrder(entity.id).map { it.toDomain() }
                entity.toDomain(items)
            }
        }
    }

    override fun getAllOrdersFlow(): Flow<List<KitchenOrder>> {
        return kitchenOrderDao.getAllKitchenOrdersFlow().map { entities ->
            entities.map { entity ->
                val items = kitchenOrderDao.getItemsForKitchenOrder(entity.id).map { it.toDomain() }
                entity.toDomain(items)
            }
        }
    }

    override suspend fun getAllStationConfigs(): List<StationConfig> {
        return stationConfigDao.getAllConfigs().map { it.toDomain() }
    }

    override fun getStationConfigsFlow(): Flow<List<StationConfig>> {
        return stationConfigDao.getAllConfigsFlow().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getStationConfig(station: WorkStation): StationConfig? {
        return stationConfigDao.getConfigById(station.id)?.toDomain()
    }

    override suspend fun saveStationConfig(config: StationConfig) {
        stationConfigDao.insertConfig(config.toEntity())
    }

    override suspend fun seedDefaultStationConfigsIfEmpty() {
        if (stationConfigDao.getConfigCount() == 0) {
            val defaults = listOf(
                StationConfig(
                    station = WorkStation.BAR,
                    name = "Barista Bar",
                    printerType = PrinterType.MOCK,
                    targetAddress = "MOCK",
                    autoPrintOnDispatch = true,
                    categoryIds = listOf("cat_coffee", "cat_non_coffee", "cat_tea", "cat_signature", "cat_beverage")
                ),
                StationConfig(
                    station = WorkStation.KITCHEN,
                    name = "Kitchen Dapur",
                    printerType = PrinterType.MOCK,
                    targetAddress = "MOCK",
                    autoPrintOnDispatch = true,
                    categoryIds = listOf("cat_food", "cat_snack", "cat_main", "cat_side")
                ),
                StationConfig(
                    station = WorkStation.DESSERT,
                    name = "Dessert & Pastry",
                    printerType = PrinterType.MOCK,
                    targetAddress = "MOCK",
                    autoPrintOnDispatch = true,
                    categoryIds = listOf("cat_pastry", "cat_dessert", "cat_bakery")
                )
            )
            defaults.forEach { saveStationConfig(it) }
        }
    }

    // ========================================================================
    // MAPPERS
    // ========================================================================

    private fun KitchenOrder.toEntity() = KitchenOrderEntity(
        id = id,
        orderId = orderId,
        orderNumber = orderNumber,
        stationId = station.id,
        status = status.name,
        customerNotes = customerNotes,
        cashierName = cashierName,
        orderTime = orderTime,
        acceptedTime = acceptedTime,
        readyTime = readyTime,
        servedTime = servedTime,
        createdAt = createdAt
    )

    private fun KitchenOrderEntity.toDomain(items: List<KitchenOrderItem>) = KitchenOrder(
        id = id,
        orderId = orderId,
        orderNumber = orderNumber,
        station = WorkStation.fromId(stationId),
        status = runCatching { KitchenOrderStatus.valueOf(status) }.getOrDefault(KitchenOrderStatus.NEW),
        items = items,
        customerNotes = customerNotes,
        cashierName = cashierName,
        orderTime = orderTime,
        acceptedTime = acceptedTime,
        readyTime = readyTime,
        servedTime = servedTime,
        createdAt = createdAt
    )

    private fun KitchenOrderItem.toEntity(kitchenOrderId: String) = KitchenOrderItemEntity(
        id = id,
        kitchenOrderId = kitchenOrderId,
        orderItemId = orderItemId,
        productId = productId,
        productName = productName,
        variantName = variantName,
        modifiersSummary = modifiersSummary,
        quantity = quantity,
        notes = notes
    )

    private fun KitchenOrderItemEntity.toDomain() = KitchenOrderItem(
        id = id,
        kitchenOrderId = kitchenOrderId,
        orderItemId = orderItemId,
        productId = productId,
        productName = productName,
        variantName = variantName,
        modifiersSummary = modifiersSummary,
        quantity = quantity,
        notes = notes
    )

    private fun StationConfig.toEntity() = StationConfigEntity(
        stationId = station.id,
        name = name,
        printerType = printerType.name,
        targetAddress = targetAddress,
        autoPrintOnDispatch = autoPrintOnDispatch,
        categoryIdsJson = categoryIds.joinToString(",")
    )

    private fun StationConfigEntity.toDomain() = StationConfig(
        station = WorkStation.fromId(stationId),
        name = name,
        printerType = runCatching { PrinterType.valueOf(printerType) }.getOrDefault(PrinterType.MOCK),
        targetAddress = targetAddress,
        autoPrintOnDispatch = autoPrintOnDispatch,
        categoryIds = categoryIdsJson.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    )
}
