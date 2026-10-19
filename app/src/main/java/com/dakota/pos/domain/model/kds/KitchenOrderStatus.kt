package com.dakota.pos.domain.model.kds

enum class KitchenOrderStatus(val displayName: String) {
    NEW("Baru"),
    ACCEPTED("Diterima"),
    PREPARING("Diproses"),
    READY("Siap Saji"),
    SERVED("Disajikan"),
    CANCELLED("Dibatalkan")
}
