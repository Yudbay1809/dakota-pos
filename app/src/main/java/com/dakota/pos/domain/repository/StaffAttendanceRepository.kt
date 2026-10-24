package com.dakota.pos.domain.repository

import com.dakota.pos.domain.model.staff.StaffAttendance
import com.dakota.pos.domain.model.staff.StaffPayrollSummary
import com.dakota.pos.domain.model.staff.StaffSalaryRate
import kotlinx.coroutines.flow.Flow

interface StaffAttendanceRepository {
    fun getAllAttendancesFlow(): Flow<List<StaffAttendance>>
    suspend fun getAllAttendances(): List<StaffAttendance>
    suspend fun recordAttendance(attendance: StaffAttendance)
    suspend fun recordBatchAttendances(attendances: List<StaffAttendance>)
    suspend fun getAttendancesByUser(userId: String): List<StaffAttendance>
    suspend fun getAttendancesByUserAndPeriod(userId: String, startTime: Long, endTime: Long): List<StaffAttendance>
    suspend fun deleteAttendance(id: String)
    
    // Payroll summary storage
    fun getAllPayrollsFlow(): Flow<List<StaffPayrollSummary>>
    suspend fun savePayroll(payroll: StaffPayrollSummary)
    suspend fun getPayrollsByPeriod(period: String): List<StaffPayrollSummary>
    
    // Salary rates
    suspend fun getSalaryRates(): List<StaffSalaryRate>
    suspend fun getSalaryRateForUser(userId: String): StaffSalaryRate
}
