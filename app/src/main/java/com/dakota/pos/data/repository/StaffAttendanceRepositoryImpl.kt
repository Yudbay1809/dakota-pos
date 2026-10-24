package com.dakota.pos.data.repository

import com.dakota.pos.data.local.dao.StaffAttendanceDao
import com.dakota.pos.data.local.dao.StaffPayrollDao
import com.dakota.pos.data.local.dao.UserDao
import com.dakota.pos.data.local.entity.StaffAttendanceEntity
import com.dakota.pos.data.local.entity.StaffPayrollEntity
import com.dakota.pos.domain.auth.UserRole
import com.dakota.pos.domain.model.staff.*
import com.dakota.pos.domain.repository.StaffAttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class StaffAttendanceRepositoryImpl(
    private val staffAttendanceDao: StaffAttendanceDao,
    private val staffPayrollDao: StaffPayrollDao,
    private val userDao: UserDao
) : StaffAttendanceRepository {

    override fun getAllAttendancesFlow(): Flow<List<StaffAttendance>> {
        return staffAttendanceDao.getAllAttendancesFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllAttendances(): List<StaffAttendance> {
        return staffAttendanceDao.getAllAttendances().map { it.toDomain() }
    }

    override suspend fun recordAttendance(attendance: StaffAttendance) {
        staffAttendanceDao.insert(attendance.toEntity())
    }

    override suspend fun recordBatchAttendances(attendances: List<StaffAttendance>) {
        staffAttendanceDao.insertAll(attendances.map { it.toEntity() })
    }

    override suspend fun getAttendancesByUser(userId: String): List<StaffAttendance> {
        return staffAttendanceDao.getAttendancesByUser(userId).map { it.toDomain() }
    }

    override suspend fun getAttendancesByUserAndPeriod(
        userId: String,
        startTime: Long,
        endTime: Long
    ): List<StaffAttendance> {
        return staffAttendanceDao.getAttendancesByUserAndPeriod(userId, startTime, endTime).map { it.toDomain() }
    }

    override suspend fun deleteAttendance(id: String) {
        staffAttendanceDao.deleteById(id)
    }

    override fun getAllPayrollsFlow(): Flow<List<StaffPayrollSummary>> {
        return staffPayrollDao.getAllPayrollsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun savePayroll(payroll: StaffPayrollSummary) {
        staffPayrollDao.insert(payroll.toEntity())
    }

    override suspend fun getPayrollsByPeriod(period: String): List<StaffPayrollSummary> {
        return staffPayrollDao.getPayrollsByPeriod(period).map { it.toDomain() }
    }

    override suspend fun getSalaryRates(): List<StaffSalaryRate> {
        val users = userDao.getAllActiveUsers().first()
        return users.map { user ->
            getSalaryRateForUser(user.id)
        }
    }

    override suspend fun getSalaryRateForUser(userId: String): StaffSalaryRate {
        val user = userDao.getUserById(userId)
        val role = user?.role ?: UserRole.BARISTA
        val name = user?.fullName ?: "Staff Dakota"

        // Skema standar kompensasi Dakota Coffee Shop
        val baseSalary = when (role) {
            UserRole.OWNER -> 10_000_000.0
            UserRole.MANAGER -> 6_500_000.0
            UserRole.CASHIER -> 4_200_000.0
            UserRole.BARISTA -> 4_000_000.0
        }

        return StaffSalaryRate(
            userId = userId,
            staffName = name,
            role = role,
            baseSalary = baseSalary,
            mealTransportPerDay = 35_000.0,
            overtimeRatePerHour = 25_000.0,
            latePenaltyPerIncident = 15_000.0,
            standardWorkingDays = 24
        )
    }

    // Mapping Functions
    private fun StaffAttendanceEntity.toDomain(): StaffAttendance {
        return StaffAttendance(
            id = id,
            userId = userId,
            staffName = staffName,
            role = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.BARISTA),
            biometricId = biometricId,
            timestamp = timestamp,
            type = runCatching { AttendanceType.valueOf(type) }.getOrDefault(AttendanceType.IN),
            status = runCatching { AttendanceStatus.valueOf(status) }.getOrDefault(AttendanceStatus.ON_TIME),
            source = runCatching { AttendanceSource.valueOf(source) }.getOrDefault(AttendanceSource.BIOMETRIC_DEVICE),
            lateMinutes = lateMinutes,
            overtimeHours = overtimeHours,
            notes = notes
        )
    }

    private fun StaffAttendance.toEntity(): StaffAttendanceEntity {
        return StaffAttendanceEntity(
            id = id,
            userId = userId,
            staffName = staffName,
            role = role.name,
            biometricId = biometricId,
            timestamp = timestamp,
            type = type.name,
            status = status.name,
            source = source.name,
            lateMinutes = lateMinutes,
            overtimeHours = overtimeHours,
            notes = notes
        )
    }

    private fun StaffPayrollEntity.toDomain(): StaffPayrollSummary {
        return StaffPayrollSummary(
            id = id,
            userId = userId,
            staffName = staffName,
            role = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.BARISTA),
            periodMonth = periodMonth,
            baseSalary = baseSalary,
            presentDays = presentDays,
            standardDays = standardDays,
            totalMealTransport = totalMealTransport,
            overtimeHours = overtimeHours,
            totalOvertimePay = totalOvertimePay,
            lateCount = lateCount,
            totalLatePenalty = totalLatePenalty,
            otherDeductions = otherDeductions,
            netSalary = netSalary,
            isPaid = isPaid,
            generatedAt = generatedAt,
            biometricTerminalId = biometricTerminalId
        )
    }

    private fun StaffPayrollSummary.toEntity(): StaffPayrollEntity {
        return StaffPayrollEntity(
            id = id,
            userId = userId,
            staffName = staffName,
            role = role.name,
            periodMonth = periodMonth,
            baseSalary = baseSalary,
            presentDays = presentDays,
            standardDays = standardDays,
            totalMealTransport = totalMealTransport,
            overtimeHours = overtimeHours,
            totalOvertimePay = totalOvertimePay,
            lateCount = lateCount,
            totalLatePenalty = totalLatePenalty,
            otherDeductions = otherDeductions,
            netSalary = netSalary,
            isPaid = isPaid,
            generatedAt = generatedAt,
            biometricTerminalId = biometricTerminalId
        )
    }
}
