package com.dakota.pos.domain.model.inventory

import com.dakota.pos.domain.model.unit.UnitType

data class StockOpnameItem(
    val id: String,
    val opnameId: String,
    val ingredientId: String,
    val ingredientName: String,
    val systemStock: Double,
    val physicalStock: Double,
    val difference: Double, // physicalStock - systemStock
    val unit: UnitType,
    val notes: String? = null
)

data class StockOpname(
    val id: String,
    val opnameNumber: String,
    val performedByUserId: String,
    val performedByUserName: String,
    val items: List<StockOpnameItem> = emptyList(),
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
