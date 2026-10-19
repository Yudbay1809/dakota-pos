package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredients")
data class IngredientEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val sku: String,
    val category: String = "Bahan Baku",
    val defaultUnit: String, // e.g. "g", "kg", "ml", "L", "pcs"
    val costPerDefaultUnit: Double,
    val minimumStock: Double = 0.0,
    val currentStock: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
