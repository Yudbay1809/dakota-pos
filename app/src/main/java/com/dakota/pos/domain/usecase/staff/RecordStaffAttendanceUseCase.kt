package com.dakota.pos.domain.usecase.staff

import com.dakota.pos.domain.auth.UserRole
import com.dakota.pos.domain.model.staff.*
import com.dakota.pos.domain.repository.StaffAttendanceRepository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class RecordStaffAttendanceUseCase(
    private val repository: StaffAttendanceRepository
) {
    suspend operator fun invoke(
        userId: String,
        staffName: String,
        role: UserRole,
        biometricId: String? = null,
        type: AttendanceType,
        source: AttendanceSource = AttendanceSource.BIOMETRIC_DEVICE,
        customTimestamp: Long? = null,
        notes: String? = null
    ): StaffAttendance {
        val now = customTimestamp ?: System.currentTimeMillis()
        val localTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault())
        val hour = localTime.hour
        val minute = localTime.minute

        var status = AttendanceStatus.NORMAL
        var lateMinutes = 0
        var overtimeHours = 0

        if (type == AttendanceType.IN) {
            // Jam Masuk Standar: 08:00
            if (hour > 8 || (hour == 8 && minute > 0)) {
                status = AttendanceStatus.LATE
                lateMinutes = (hour - 8) * 60 + minute
            } else {
                status = AttendanceStatus.ON_TIME
            }
        } else {
            // Jam Pulang Standar: 17:00
            if (hour >= 18) {
                status = AttendanceStatus.OVERTIME
                overtimeHours = hour - 17
            } else {
                status = AttendanceStatus.NORMAL
            }
        }

        if (source == AttendanceSource.MANAGER_OVERRIDE) {
            status = AttendanceStatus.MANUAL_ADJUSTMENT
        }

        val attendance = StaffAttendance(
            id = "ATT-" + UUID.randomUUID().toString().take(8).uppercase(),
            userId = userId,
            staffName = staffName,
            role = role,
            biometricId = biometricId,
            timestamp = now,
            type = type,
            status = status,
            source = source,
            lateMinutes = lateMinutes,
            overtimeHours = overtimeHours,
            notes = notes
        )

        repository.recordAttendance(attendance)
        return attendance
    }
}
