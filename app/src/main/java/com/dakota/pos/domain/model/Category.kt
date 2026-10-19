package com.dakota.pos.domain.model

data class Category(
    val id: String,
    val name: String,
    val displayOrder: Int = 0,
    val iconName: String? = null
)
