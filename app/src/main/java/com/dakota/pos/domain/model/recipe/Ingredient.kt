package com.dakota.pos.domain.model.recipe

import com.dakota.pos.domain.model.unit.UnitType

/**
 * Entitas Bahan Baku Mentah (Ingredient).
 * Sesuai Cardinal Rule 12: Product TIDAK SAMA DENGAN Ingredient.
 */
data class Ingredient(
    val id: String,
    val name: String,
    val sku: String,
    val category: String = "Bahan Baku",
    val defaultUnit: UnitType,
    val costPerDefaultUnit: Double,
    val minimumStock: Double = 0.0,
    val currentStock: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
