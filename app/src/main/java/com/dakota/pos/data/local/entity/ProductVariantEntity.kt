package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_variants",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productId")]
)
data class ProductVariantEntity(
    @PrimaryKey
    val id: String,
    val productId: String,
    val name: String, // e.g., "Hot / Regular", "Iced / Large"
    val priceAdjustment: Double = 0.0,
    val isDefault: Boolean = false
)
