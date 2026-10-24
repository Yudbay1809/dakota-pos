package com.dakota.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staff_payroll")
data class StaffPayrollEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val staffName: String,
    val role: String,
    val periodMonth: String,
    val baseSalary: Double,
    val presentDays: Int,
    val standardDays: Int,
    val totalMealTransport: Double,
    val overtimeHours: Int,
    val totalOvertimePay: Double,
    val lateCount: Int,
    val totalLatePenalty: Double,
    val otherDeductions: Double,
    val netSalary: Double,
    val isPaid: Boolean,
    val generatedAt: Long,
    val biometricTerminalId: String
)
