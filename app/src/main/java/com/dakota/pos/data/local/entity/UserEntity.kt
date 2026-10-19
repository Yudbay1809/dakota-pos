package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dakota.pos.domain.auth.UserRole

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val username: String,
    val fullName: String,
    val role: UserRole,
    val pinHash: String, // SHA-256 hashed PIN
    val passwordHash: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
