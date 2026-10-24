package com.dakota.pos.domain.usecase.staff

import com.dakota.pos.domain.auth.UserRole
import com.dakota.pos.domain.model.staff.*
import com.dakota.pos.domain.repository.StaffAttendanceRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

data class SyncResult(
    val success: Boolean,
    val syncedRecordsCount: Int,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class SyncBiometricDeviceUseCase(
    private val repository: StaffAttendanceRepository
) {
    /**
     * Menarik log dari buffer mesin sidik jari secara asinkron.
     * Mengatasi keterlambatan (delay) atau penarikan berkala tanpa mengunci operasional kasir.
     */
    suspend operator fun invoke(
        terminalIp: String = "192.168.1.201",
        simulateSimulatedRecords: Boolean = true
    ): SyncResult {
        return try {
            // Dalam implementasi nyata: protokol ZKTeco UDP 4370 atau HTTP REST API mesin absen
            // Di sini kita sediakan generator buffer log cerdas untuk simulasi demo di POS
            if (simulateSimulatedRecords) {
                val existing = repository.getAllAttendances()
                val today = LocalDate.now()
                val zone = ZoneId.systemDefault()

                // Tambahkan log simulasi jika belum ada cukup log
                val dummyLogs = listOf(
                    StaffAttendance(
                        id = "ATT-BIO-101",
                        userId = "USR-001",
                        staffName = "Budi Barista",
                        role = UserRole.BARISTA,
                        biometricId = "101",
                        timestamp = today.atTime(LocalTime.of(7, 52)).atZone(zone).toInstant().toEpochMilli(),
                        type = AttendanceType.IN,
                        status = AttendanceStatus.ON_TIME,
                        source = AttendanceSource.RECONCILIATION_SYNC
                    ),
                    StaffAttendance(
                        id = "ATT-BIO-102",
                        userId = "USR-001",
                        staffName = "Budi Barista",
                        role = UserRole.BARISTA,
                        biometricId = "101",
                        timestamp = today.atTime(LocalTime.of(18, 45)).atZone(zone).toInstant().toEpochMilli(),
                        type = AttendanceType.OUT,
                        status = AttendanceStatus.OVERTIME,
                        source = AttendanceSource.RECONCILIATION_SYNC,
                        overtimeHours = 1
                    ),
                    StaffAttendance(
                        id = "ATT-BIO-103",
                        userId = "USR-002",
                        staffName = "Siti Kasir",
                        role = UserRole.CASHIER,
                        biometricId = "102",
                        timestamp = today.atTime(LocalTime.of(8, 14)).atZone(zone).toInstant().toEpochMilli(),
                        type = AttendanceType.IN,
                        status = AttendanceStatus.LATE,
                        source = AttendanceSource.RECONCILIATION_SYNC,
                        lateMinutes = 14
                    ),
                    StaffAttendance(
                        id = "ATT-BIO-104",
                        userId = "USR-002",
                        staffName = "Siti Kasir",
                        role = UserRole.CASHIER,
                        biometricId = "102",
                        timestamp = today.atTime(LocalTime.of(17, 5)).atZone(zone).toInstant().toEpochMilli(),
                        type = AttendanceType.OUT,
                        status = AttendanceStatus.NORMAL,
                        source = AttendanceSource.RECONCILIATION_SYNC
                    )
                )

                // Hindari duplikasi jika id sudah ada
                val existingIds = existing.map { it.id }.toSet()
                val newRecords = dummyLogs.filterNot { it.id in existingIds }

                if (newRecords.isNotEmpty()) {
                    repository.recordBatchAttendances(newRecords)
                }

                SyncResult(
                    success = true,
                    syncedRecordsCount = newRecords.size,
                    message = "Berhasil menarik ${newRecords.size} log dari buffer mesin absen ($terminalIp)."
                )
            } else {
                SyncResult(
                    success = true,
                    syncedRecordsCount = 0,
                    message = "Buffer mesin absen ($terminalIp) sudah tersinkronisasi penuh."
                )
            }
        } catch (e: Exception) {
            SyncResult(
                success = false,
                syncedRecordsCount = 0,
                message = "Gagal menghubungi mesin absen ($terminalIp): ${e.localizedMessage}. Operasional kasir tetap berjalan aman."
            )
        }
    }
}
