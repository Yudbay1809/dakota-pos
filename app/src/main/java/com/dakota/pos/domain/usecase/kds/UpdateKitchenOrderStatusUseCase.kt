package com.dakota.pos.domain.usecase.kds

import com.dakota.pos.domain.model.kds.KitchenOrderStatus
import com.dakota.pos.domain.repository.KdsRepository

class UpdateKitchenOrderStatusUseCase(
    private val kdsRepository: KdsRepository
) {
    suspend operator fun invoke(orderId: String, newStatus: KitchenOrderStatus): Result<Unit> {
        val order = kdsRepository.getKitchenOrderById(orderId)
            ?: return Result.failure(IllegalArgumentException("Pesanan dapur tidak ditemukan: $orderId"))

        val isValidTransition = when (order.status) {
            KitchenOrderStatus.NEW -> newStatus in listOf(KitchenOrderStatus.ACCEPTED, KitchenOrderStatus.PREPARING, KitchenOrderStatus.CANCELLED)
            KitchenOrderStatus.ACCEPTED -> newStatus in listOf(KitchenOrderStatus.PREPARING, KitchenOrderStatus.READY, KitchenOrderStatus.CANCELLED)
            KitchenOrderStatus.PREPARING -> newStatus in listOf(KitchenOrderStatus.READY, KitchenOrderStatus.CANCELLED)
            KitchenOrderStatus.READY -> newStatus in listOf(KitchenOrderStatus.SERVED)
            KitchenOrderStatus.SERVED -> false
            KitchenOrderStatus.CANCELLED -> false
        }

        if (!isValidTransition) {
            return Result.failure(IllegalStateException("Transisi status tidak valid dari ${order.status} ke $newStatus"))
        }

        kdsRepository.updateOrderStatus(orderId, newStatus)
        return Result.success(Unit)
    }

    suspend fun advanceStatus(orderId: String): Result<KitchenOrderStatus> {
        val order = kdsRepository.getKitchenOrderById(orderId)
            ?: return Result.failure(IllegalArgumentException("Pesanan dapur tidak ditemukan: $orderId"))

        val nextStatus = when (order.status) {
            KitchenOrderStatus.NEW -> KitchenOrderStatus.ACCEPTED
            KitchenOrderStatus.ACCEPTED -> KitchenOrderStatus.PREPARING
            KitchenOrderStatus.PREPARING -> KitchenOrderStatus.READY
            KitchenOrderStatus.READY -> KitchenOrderStatus.SERVED
            KitchenOrderStatus.SERVED -> return Result.failure(IllegalStateException("Pesanan sudah selesai disajikan"))
            KitchenOrderStatus.CANCELLED -> return Result.failure(IllegalStateException("Pesanan telah dibatalkan"))
        }

        val result = invoke(orderId, nextStatus)
        return if (result.isSuccess) Result.success(nextStatus) else Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
    }
}
