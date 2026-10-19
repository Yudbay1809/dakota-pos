package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val username: String,
    val action: String, // e.g. "LOGIN_SUCCESS", "LOGIN_FAILED", "LOGOUT", "PERMISSION_DENIED"
    val details: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
