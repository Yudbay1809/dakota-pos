package com.dakota.pos.domain.usecase.kds

import com.dakota.pos.domain.model.kds.KitchenOrder
import com.dakota.pos.domain.model.kds.WorkStation
import com.dakota.pos.domain.repository.KdsRepository
import kotlinx.coroutines.flow.Flow

class GetKdsOrdersUseCase(
    private val kdsRepository: KdsRepository
) {
    operator fun invoke(station: WorkStation? = null): Flow<List<KitchenOrder>> {
        return if (station == null) {
            kdsRepository.getActiveOrdersFlow()
        } else {
            kdsRepository.getActiveOrdersByStationFlow(station)
        }
    }

    fun getAllOrders(): Flow<List<KitchenOrder>> {
        return kdsRepository.getAllOrdersFlow()
    }
}
