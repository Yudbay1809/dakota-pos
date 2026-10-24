package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dakota.pos.domain.auth.UserRole
import com.dakota.pos.domain.model.staff.AttendanceSource
import com.dakota.pos.domain.model.staff.AttendanceStatus
import com.dakota.pos.domain.model.staff.AttendanceType

@Entity(tableName = "staff_attendance")
data class StaffAttendanceEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val staffName: String,
    val role: String,
    val biometricId: String?,
    val timestamp: Long,
    val type: String, // IN, OUT
    val status: String, // ON_TIME, LATE, OVERTIME, NORMAL, MANUAL_ADJUSTMENT
    val source: String, // BIOMETRIC_DEVICE, etc.
    val lateMinutes: Int = 0,
    val overtimeHours: Int = 0,
    val notes: String? = null
)
