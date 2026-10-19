package com.dakota.pos.domain.model.recipe

import com.dakota.pos.domain.model.unit.UnitType

/**
 * Item komponen bahan baku dalam suatu resep.
 */
data class RecipeItem(
    val id: String,
    val recipeId: String,
    val ingredientId: String,
    val ingredientName: String,
    val quantity: Double,
    val unit: UnitType,
    val notes: String? = null
)
