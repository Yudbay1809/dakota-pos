package com.dakota.pos.domain.model

data class Modifier(
    val id: String,
    val name: String,
    val groupName: String,
    val price: Double = 0.0,
    val isActive: Boolean = true
)
