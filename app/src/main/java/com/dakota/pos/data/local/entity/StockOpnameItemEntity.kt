package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_opname_items",
    foreignKeys = [
        ForeignKey(
            entity = StockOpnameEntity::class,
            parentColumns = ["id"],
            childColumns = ["opnameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("opnameId")]
)
data class StockOpnameItemEntity(
    @PrimaryKey
    val id: String,
    val opnameId: String,
    val ingredientId: String,
    val ingredientName: String,
    val systemStock: Double,
    val physicalStock: Double,
    val difference: Double,
    val unit: String,
    val notes: String? = null
)
