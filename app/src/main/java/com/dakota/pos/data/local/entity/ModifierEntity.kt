package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "modifiers")
data class ModifierEntity(
    @PrimaryKey
    val id: String,
    val name: String, // e.g., "Extra Espresso Shot (+5k)", "Oat Milk (+8k)", "Less Sugar (Free)"
    val groupName: String, // e.g., "Milk Alternative", "Sweetness", "Add-On"
    val price: Double = 0.0,
    val isActive: Boolean = true
)
