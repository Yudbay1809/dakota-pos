package com.dakota.pos.domain.model.staff

import com.dakota.pos.domain.auth.UserRole

enum class AttendanceType {
    IN,  // Clock-in / Masuk
    OUT  // Clock-out / Pulang
}

enum class AttendanceStatus {
    ON_TIME,           // Tepat waktu (<= 08:00)
    LATE,              // Terlambat (> 08:00)
    OVERTIME,          // Lembur (> 17:00 / > 8 jam)
    NORMAL,            // Pulang normal
    MANUAL_ADJUSTMENT  // Disetujui/dikoreksi manual oleh Manager
}

enum class AttendanceSource {
    BIOMETRIC_DEVICE,      // Mesin sidik jari fisik
    POS_MANUAL_SIMULATION, // Tap sensor di tablet POS
    RECONCILIATION_SYNC,   // Penarikan asinkron dari buffer mesin
    MANAGER_OVERRIDE       // Koreksi izin/sakit/lupa scan oleh Manager
}

data class StaffAttendance(
    val id: String,
    val userId: String,
    val staffName: String,
    val role: UserRole = UserRole.BARISTA,
    val biometricId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val type: AttendanceType,
    val status: AttendanceStatus,
    val source: AttendanceSource = AttendanceSource.BIOMETRIC_DEVICE,
    val lateMinutes: Int = 0,
    val overtimeHours: Int = 0,
    val notes: String? = null
)

data class StaffSalaryRate(
    val userId: String,
    val staffName: String,
    val role: UserRole,
    val baseSalary: Double,
    val mealTransportPerDay: Double = 35_000.0,
    val overtimeRatePerHour: Double = 25_000.0,
    val latePenaltyPerIncident: Double = 15_000.0,
    val standardWorkingDays: Int = 24
)

data class StaffPayrollSummary(
    val id: String,
    val userId: String,
    val staffName: String,
    val role: UserRole,
    val periodMonth: String, // e.g. "Mei 2026"
    val baseSalary: Double,
    val presentDays: Int,
    val standardDays: Int = 24,
    val totalMealTransport: Double,
    val overtimeHours: Int,
    val totalOvertimePay: Double,
    val lateCount: Int,
    val totalLatePenalty: Double,
    val otherDeductions: Double = 0.0,
    val netSalary: Double,
    val isPaid: Boolean = false,
    val generatedAt: Long = System.currentTimeMillis(),
    val biometricTerminalId: String = "TERMINAL-DAKOTA-01"
)
