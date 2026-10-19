package com.dakota.pos.domain.model.kds

enum class WorkStation(
    val id: String,
    val displayName: String,
    val defaultTarget: String
) {
    BAR("station_bar", "Barista Bar", "BT:00:11:22:33:44:55"),
    KITCHEN("station_kitchen", "Kitchen Dapur", "TCP:192.168.1.200:9100"),
    DESSERT("station_dessert", "Dessert & Pastry", "MOCK");

    companion object {
        fun fromId(id: String): WorkStation {
            return entries.find { it.id.equals(id, ignoreCase = true) }
                ?: entries.find { it.name.equals(id, ignoreCase = true) }
                ?: BAR
        }
    }
}
