package com.dakota.pos.domain.usecase.staff

import com.dakota.pos.data.local.dao.OperatingExpenseDao
import com.dakota.pos.data.local.entity.OperatingExpenseEntity
import com.dakota.pos.domain.model.staff.StaffPayrollSummary
import com.dakota.pos.domain.repository.StaffAttendanceRepository
import java.util.UUID

class SaveStaffPayrollUseCase(
    private val staffAttendanceRepository: StaffAttendanceRepository,
    private val operatingExpenseDao: OperatingExpenseDao? = null
) {
    suspend operator fun invoke(
        payroll: StaffPayrollSummary,
        postToOperatingExpenses: Boolean = true
    ) {
        // Simpan catatan slip gaji
        staffAttendanceRepository.savePayroll(payroll)

        // Posting otomatis ke modul Beban Operasional (P&L) POS
        if (postToOperatingExpenses && operatingExpenseDao != null) {
            val expense = OperatingExpenseEntity(
                id = "EXP-SAL-" + UUID.randomUUID().toString().take(8).uppercase(),
                category = "Gaji & Upah",
                amount = payroll.netSalary,
                description = "Gaji Staf ${payroll.staffName} (${payroll.role.name}) - Periode ${payroll.periodMonth}",
                date = payroll.generatedAt,
                recordedBy = "Manager / Owner",
                notes = "Otomasi slip gaji dari audit presensi sidik jari",
                createdAt = System.currentTimeMillis()
            )
            operatingExpenseDao.insertExpense(expense)
        }
    }
}
