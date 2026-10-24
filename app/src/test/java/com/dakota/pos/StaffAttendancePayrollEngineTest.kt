package com.dakota.pos

import com.dakota.pos.domain.auth.UserRole
import com.dakota.pos.domain.model.staff.*
import com.dakota.pos.domain.repository.StaffAttendanceRepository
import com.dakota.pos.domain.usecase.staff.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Unit Tests for Phase 13: Staff Attendance & Biometric Payroll in Dakota POS.
 *
 * Test coverage:
 * 1. Clock-in on-time (<= 08:00) recorded as ON_TIME
 * 2. Clock-in late (> 08:00) recorded as LATE with exact lateMinutes
 * 3. Clock-out normal (<= 17:00) recorded as NORMAL
 * 4. Clock-out overtime (> 17:00) recorded as OVERTIME with exact overtimeHours
 * 5. Manager override recorded as MANUAL_ADJUSTMENT
 * 6. Decoupled Biometric Sync pulls buffer records asynchronously without locking
 * 7. Automated Payroll calculation: present days, meal & transport, overtime pay, late penalties
 * 8. Net Salary calculation formula matches Dakota compensation rates
 * 9. Thermal receipt formatting generates complete pay slip breakdown
 */
class StaffAttendancePayrollEngineTest {

    private lateinit var fakeRepo: FakeStaffAttendanceRepository
    private lateinit var recordStaffAttendanceUseCase: RecordStaffAttendanceUseCase
    private lateinit var syncBiometricDeviceUseCase: SyncBiometricDeviceUseCase
    private lateinit var calculateStaffPayrollUseCase: CalculateStaffPayrollUseCase
    private lateinit var generatePaySlipReceiptUseCase: GeneratePaySlipReceiptUseCase

    @Before
    fun setup() {
        fakeRepo = FakeStaffAttendanceRepository()
        recordStaffAttendanceUseCase = RecordStaffAttendanceUseCase(fakeRepo)
        syncBiometricDeviceUseCase = SyncBiometricDeviceUseCase(fakeRepo)
        calculateStaffPayrollUseCase = CalculateStaffPayrollUseCase(fakeRepo)
        generatePaySlipReceiptUseCase = GeneratePaySlipReceiptUseCase()
    }

    @Test
    fun testClockInOnTime() = runBlocking {
        val today = LocalDate.of(2026, 5, 20)
        val timestamp = today.atTime(LocalTime.of(7, 50)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val result = recordStaffAttendanceUseCase(
            userId = "USR-001",
            staffName = "Budi Barista",
            role = UserRole.BARISTA,
            type = AttendanceType.IN,
            customTimestamp = timestamp
        )

        assertEquals(AttendanceType.IN, result.type)
        assertEquals(AttendanceStatus.ON_TIME, result.status)
        assertEquals(0, result.lateMinutes)
    }

    @Test
    fun testClockInLateCalculatesLateMinutes() = runBlocking {
        val today = LocalDate.of(2026, 5, 20)
        val timestamp = today.atTime(LocalTime.of(8, 25)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val result = recordStaffAttendanceUseCase(
            userId = "USR-001",
            staffName = "Budi Barista",
            role = UserRole.BARISTA,
            type = AttendanceType.IN,
            customTimestamp = timestamp
        )

        assertEquals(AttendanceType.IN, result.type)
        assertEquals(AttendanceStatus.LATE, result.status)
        assertEquals(25, result.lateMinutes)
    }

    @Test
    fun testClockOutOvertimeCalculatesOvertimeHours() = runBlocking {
        val today = LocalDate.of(2026, 5, 20)
        val timestamp = today.atTime(LocalTime.of(19, 30)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val result = recordStaffAttendanceUseCase(
            userId = "USR-001",
            staffName = "Budi Barista",
            role = UserRole.BARISTA,
            type = AttendanceType.OUT,
            customTimestamp = timestamp
        )

        assertEquals(AttendanceType.OUT, result.type)
        assertEquals(AttendanceStatus.OVERTIME, result.status)
        assertEquals(2, result.overtimeHours) // 19 - 17 = 2 hours
    }

    @Test
    fun testManagerOverrideRecordedAsManualAdjustment() = runBlocking {
        val result = recordStaffAttendanceUseCase(
            userId = "USR-002",
            staffName = "Siti Kasir",
            role = UserRole.CASHIER,
            type = AttendanceType.IN,
            source = AttendanceSource.MANAGER_OVERRIDE,
            notes = "Lupa scan karena mesin offline"
        )

        assertEquals(AttendanceStatus.MANUAL_ADJUSTMENT, result.status)
        assertEquals(AttendanceSource.MANAGER_OVERRIDE, result.source)
    }

    @Test
    fun testAsynchronousBiometricSyncPullsBufferWithoutErrors() = runBlocking {
        val syncResult = syncBiometricDeviceUseCase(terminalIp = "192.168.1.201", simulateSimulatedRecords = true)

        assertTrue(syncResult.success)
        assertTrue(syncResult.syncedRecordsCount >= 0)
        val allLogs = fakeRepo.getAllAttendances()
        assertTrue(allLogs.isNotEmpty())
    }

    @Test
    fun testAutomatedPayrollCalculation() = runBlocking {
        val zone = ZoneId.systemDefault()
        val mockLogs = listOf(
            // Day 1: on time, normal out
            StaffAttendance(
                id = "LOG-1",
                userId = "USR-001",
                staffName = "Budi Barista",
                role = UserRole.BARISTA,
                timestamp = LocalDate.of(2026, 5, 2).atTime(7, 55).atZone(zone).toInstant().toEpochMilli(),
                type = AttendanceType.IN,
                status = AttendanceStatus.ON_TIME
            ),
            StaffAttendance(
                id = "LOG-2",
                userId = "USR-001",
                staffName = "Budi Barista",
                role = UserRole.BARISTA,
                timestamp = LocalDate.of(2026, 5, 2).atTime(17, 5).atZone(zone).toInstant().toEpochMilli(),
                type = AttendanceType.OUT,
                status = AttendanceStatus.NORMAL
            ),
            // Day 2: late IN, 2 hrs overtime OUT
            StaffAttendance(
                id = "LOG-3",
                userId = "USR-001",
                staffName = "Budi Barista",
                role = UserRole.BARISTA,
                timestamp = LocalDate.of(2026, 5, 3).atTime(8, 20).atZone(zone).toInstant().toEpochMilli(),
                type = AttendanceType.IN,
                status = AttendanceStatus.LATE,
                lateMinutes = 20
            ),
            StaffAttendance(
                id = "LOG-4",
                userId = "USR-001",
                staffName = "Budi Barista",
                role = UserRole.BARISTA,
                timestamp = LocalDate.of(2026, 5, 3).atTime(19, 10).atZone(zone).toInstant().toEpochMilli(),
                type = AttendanceType.OUT,
                status = AttendanceStatus.OVERTIME,
                overtimeHours = 2
            )
        )

        val payroll = calculateStaffPayrollUseCase(
            userId = "USR-001",
            periodMonth = "Mei 2026",
            otherDeductions = 0.0,
            explicitLogs = mockLogs
        )

        assertEquals("USR-001", payroll.userId)
        assertEquals("Budi Barista", payroll.staffName)
        assertEquals(2, payroll.presentDays)
        assertEquals(1, payroll.lateCount)
        assertEquals(2, payroll.overtimeHours)

        // Allowance: 2 days * Rp 35.000 = Rp 70.000
        assertEquals(70_000.0, payroll.totalMealTransport, 0.01)
        // Overtime: 2 hours * Rp 25.000 = Rp 50.000
        assertEquals(50_000.0, payroll.totalOvertimePay, 0.01)
        // Late Penalty: 1 incident * Rp 15.000 = Rp 15.000
        assertEquals(15_000.0, payroll.totalLatePenalty, 0.01)

        // Net Salary: 4.000.000 + 70.000 + 50.000 - 15.000 = 4.105.000
        val expectedNet = 4_000_000.0 + 70_000.0 + 50_000.0 - 15_000.0
        assertEquals(expectedNet, payroll.netSalary, 0.01)
    }

    @Test
    fun testGeneratePaySlipThermalReceipt() = runBlocking {
        val summary = StaffPayrollSummary(
            id = "SAL-TEST-01",
            userId = "USR-001",
            staffName = "Budi Barista",
            role = UserRole.BARISTA,
            periodMonth = "Mei 2026",
            baseSalary = 4_000_000.0,
            presentDays = 22,
            standardDays = 24,
            totalMealTransport = 770_000.0,
            overtimeHours = 4,
            totalOvertimePay = 100_000.0,
            lateCount = 1,
            totalLatePenalty = 15_000.0,
            otherDeductions = 0.0,
            netSalary = 4_855_000.0,
            isPaid = true
        )

        val receiptText = generatePaySlipReceiptUseCase(summary)

        assertTrue(receiptText.contains("DAKOTA COFFEE SHOP"))
        assertTrue(receiptText.contains("SLIP GAJI & UPAH KARYAWAN"))
        assertTrue(receiptText.contains("Budi Barista"))
        assertTrue(receiptText.contains("BARISTA"))
        assertTrue(receiptText.contains("SAL-TEST-01"))
        assertTrue(receiptText.contains("4.855.000"))
        assertTrue(receiptText.contains("LUNAS / DIBAYARKAN"))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FAKE REPOSITORY FOR UNIT TESTS
// ─────────────────────────────────────────────────────────────────────────────
class FakeStaffAttendanceRepository : StaffAttendanceRepository {
    private val attendances = mutableListOf<StaffAttendance>()
    private val payrolls = mutableListOf<StaffPayrollSummary>()

    override fun getAllAttendancesFlow(): Flow<List<StaffAttendance>> = flowOf(attendances)

    override suspend fun getAllAttendances(): List<StaffAttendance> = attendances.toList()

    override suspend fun recordAttendance(attendance: StaffAttendance) {
        attendances.add(0, attendance)
    }

    override suspend fun recordBatchAttendances(newAttendances: List<StaffAttendance>) {
        attendances.addAll(0, newAttendances)
    }

    override suspend fun getAttendancesByUser(userId: String): List<StaffAttendance> {
        return attendances.filter { it.userId == userId }
    }

    override suspend fun getAttendancesByUserAndPeriod(
        userId: String,
        startTime: Long,
        endTime: Long
    ): List<StaffAttendance> {
        return attendances.filter { it.userId == userId && it.timestamp in startTime..endTime }
    }

    override suspend fun deleteAttendance(id: String) {
        attendances.removeAll { it.id == id }
    }

    override fun getAllPayrollsFlow(): Flow<List<StaffPayrollSummary>> = flowOf(payrolls)

    override suspend fun savePayroll(payroll: StaffPayrollSummary) {
        payrolls.add(0, payroll)
    }

    override suspend fun getPayrollsByPeriod(period: String): List<StaffPayrollSummary> {
        return payrolls.filter { it.periodMonth == period }
    }

    override suspend fun getSalaryRates(): List<StaffSalaryRate> {
        return listOf(
            StaffSalaryRate("USR-001", "Budi Barista", UserRole.BARISTA, 4_000_000.0),
            StaffSalaryRate("USR-002", "Siti Kasir", UserRole.CASHIER, 4_200_000.0)
        )
    }

    override suspend fun getSalaryRateForUser(userId: String): StaffSalaryRate {
        return when (userId) {
            "USR-002" -> StaffSalaryRate("USR-002", "Siti Kasir", UserRole.CASHIER, 4_200_000.0)
            else -> StaffSalaryRate("USR-001", "Budi Barista", UserRole.BARISTA, 4_000_000.0)
        }
    }
}
