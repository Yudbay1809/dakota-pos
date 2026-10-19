package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("categoryId")]
)
data class ProductEntity(
    @PrimaryKey
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String = "",
    val basePrice: Double,
    val colorHex: String = "#6F4E37",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
