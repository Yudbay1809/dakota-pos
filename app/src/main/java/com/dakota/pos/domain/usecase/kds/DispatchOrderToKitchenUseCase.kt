package com.dakota.pos.domain.usecase.kds

import com.dakota.pos.domain.model.Order
import com.dakota.pos.domain.model.kds.KitchenOrder
import com.dakota.pos.domain.model.kds.KitchenOrderItem
import com.dakota.pos.domain.model.kds.KitchenOrderStatus
import com.dakota.pos.domain.model.kds.WorkStation
import com.dakota.pos.domain.repository.KdsRepository
import com.dakota.pos.domain.repository.ProductRepository
import java.util.UUID

class DispatchOrderToKitchenUseCase(
    private val kdsRepository: KdsRepository,
    private val productRepository: ProductRepository,
    private val printStationTicketUseCase: PrintStationTicketUseCase
) {
    suspend operator fun invoke(order: Order): List<KitchenOrder> {
        if (order.items.isEmpty()) return emptyList()

        kdsRepository.seedDefaultStationConfigsIfEmpty()
        val stationConfigs = kdsRepository.getAllStationConfigs()

        // Build mapping from categoryId to WorkStation
        val categoryToStation = mutableMapOf<String, WorkStation>()
        stationConfigs.forEach { config ->
            config.categoryIds.forEach { catId ->
                categoryToStation[catId] = config.station
            }
        }

        // Cache product category lookups
        val productCategoryMap = mutableMapOf<String, String>()
        order.items.forEach { item ->
            if (!productCategoryMap.containsKey(item.productId)) {
                val product = productRepository.getProductById(item.productId)
                if (product != null) {
                    productCategoryMap[item.productId] = product.categoryId
                }
            }
        }

        // Group order items by target WorkStation
        val stationItemsMap = mutableMapOf<WorkStation, MutableList<KitchenOrderItem>>()

        order.items.forEach { item ->
            val catId = productCategoryMap[item.productId]
            val station = if (catId != null && categoryToStation.containsKey(catId)) {
                categoryToStation[catId]!!
            } else {
                // Fallback heuristic based on product name keywords
                when {
                    item.productName.contains("cake", ignoreCase = true) ||
                    item.productName.contains("pastry", ignoreCase = true) ||
                    item.productName.contains("croissant", ignoreCase = true) -> WorkStation.DESSERT
                    item.productName.contains("rice", ignoreCase = true) ||
                    item.productName.contains("nasi", ignoreCase = true) ||
                    item.productName.contains("mie", ignoreCase = true) ||
                    item.productName.contains("fried", ignoreCase = true) ||
                    item.productName.contains("snack", ignoreCase = true) -> WorkStation.KITCHEN
                    else -> WorkStation.BAR
                }
            }

            val kitchenItem = KitchenOrderItem(
                id = UUID.randomUUID().toString(),
                kitchenOrderId = "",
                orderItemId = item.id,
                productId = item.productId,
                productName = item.productName,
                variantName = item.variantName,
                modifiersSummary = item.selectedModifiersSummary,
                quantity = item.quantity,
                notes = item.notes
            )

            stationItemsMap.getOrPut(station) { mutableListOf() }.add(kitchenItem)
        }

        val dispatchedOrders = mutableListOf<KitchenOrder>()
        val now = System.currentTimeMillis()

        stationItemsMap.forEach { (station, items) ->
            val kitchenOrderId = UUID.randomUUID().toString()
            val fixedItems = items.map { it.copy(kitchenOrderId = kitchenOrderId) }

            val kitchenOrder = KitchenOrder(
                id = kitchenOrderId,
                orderId = order.id,
                orderNumber = order.orderNumber,
                station = station,
                status = KitchenOrderStatus.NEW,
                items = fixedItems,
                customerNotes = order.customerNotes,
                cashierName = order.cashierName,
                orderTime = now,
                createdAt = now
            )

            kdsRepository.saveKitchenOrder(kitchenOrder)
            dispatchedOrders.add(kitchenOrder)

            // Auto-print if enabled for this station
            val config = stationConfigs.find { it.station == station }
            if (config?.autoPrintOnDispatch == true) {
                printStationTicketUseCase(kitchenOrder)
            }
        }

        return dispatchedOrders
    }
}
