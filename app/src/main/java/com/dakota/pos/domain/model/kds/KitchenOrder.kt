package com.dakota.pos.domain.model.kds

data class KitchenOrder(
    val id: String,
    val orderId: String,
    val orderNumber: String,
    val station: WorkStation,
    val status: KitchenOrderStatus = KitchenOrderStatus.NEW,
    val items: List<KitchenOrderItem> = emptyList(),
    val customerNotes: String? = null,
    val cashierName: String,
    val orderTime: Long = System.currentTimeMillis(),
    val acceptedTime: Long? = null,
    val readyTime: Long? = null,
    val servedTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Durasi tunggu pesanan dalam menit dihitung dari waktu order dibuat.
     */
    val elapsedMinutes: Long
        get() {
            val end = servedTime ?: System.currentTimeMillis()
            return ((end - orderTime) / 60000L).coerceAtLeast(0L)
        }

    /**
     * Level urgensi berdasarkan lama waktu menunggu:
     * - Hijau (< 5 min): Normal
     * - Kuning (5-10 min): Warning
     * - Merah (> 10 min): Critical / Overdue
     */
    val isCritical: Boolean
        get() = elapsedMinutes >= 10L && status != KitchenOrderStatus.SERVED && status != KitchenOrderStatus.CANCELLED

    val isWarning: Boolean
        get() = elapsedMinutes in 5L..9L && status != KitchenOrderStatus.SERVED && status != KitchenOrderStatus.CANCELLED
}
