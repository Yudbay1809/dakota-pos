package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recipes",
    indices = [
        Index(value = ["productId"]),
        Index(value = ["variantId"])
    ]
)
data class RecipeEntity(
    @PrimaryKey
    val id: String,
    val productId: String,
    val productName: String,
    val variantId: String? = null,
    val variantName: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
