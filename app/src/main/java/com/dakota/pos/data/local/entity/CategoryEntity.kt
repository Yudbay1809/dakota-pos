package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String, // Coffee, Non-Coffee, Food, Dessert
    val displayOrder: Int = 0,
    val iconName: String? = null
)
