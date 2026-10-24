package com.dakota.pos.domain.usecase.staff

import com.dakota.pos.domain.model.staff.StaffPayrollSummary
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class GeneratePaySlipReceiptUseCase {

    private val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    operator fun invoke(payroll: StaffPayrollSummary): String {
        val line = "=".repeat(32)
        val dash = "-".repeat(32)
        val timeStr = LocalDateTime.ofInstant(Instant.ofEpochMilli(payroll.generatedAt), ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

        return buildString {
            appendLine("       DAKOTA COFFEE SHOP       ")
            appendLine("    SLIP GAJI & UPAH KARYAWAN   ")
            appendLine(line)
            appendLine("No. Slip   : ${payroll.id}")
            appendLine("Tanggal    : $timeStr")
            appendLine("Periode    : ${payroll.periodMonth}")
            appendLine("Nama Staf  : ${payroll.staffName}")
            appendLine("Jabatan    : ${payroll.role.name}")
            appendLine("Terminal   : ${payroll.biometricTerminalId}")
            appendLine(dash)
            appendLine("REKAP AUDIT ABSENSI:")
            appendLine(" • Kehadiran: ${payroll.presentDays} / ${payroll.standardDays} Hari")
            appendLine(" • Lembur   : ${payroll.overtimeHours} Jam")
            appendLine(" • Terlambat: ${payroll.lateCount} Kali")
            appendLine(dash)
            appendLine("RINCIAN PENGHASILAN:")
            appendLine("Gaji Pokok      : ${rupiahFormat.format(payroll.baseSalary)}")
            appendLine("Uang Makan/Trf  : ${rupiahFormat.format(payroll.totalMealTransport)}")
            appendLine("Upah Lembur     : ${rupiahFormat.format(payroll.totalOvertimePay)}")
            appendLine(dash)
            appendLine("POTONGAN:")
            appendLine("Denda Terlambat : -${rupiahFormat.format(payroll.totalLatePenalty)}")
            if (payroll.otherDeductions > 0) {
                appendLine("Potongan Lain   : -${rupiahFormat.format(payroll.otherDeductions)}")
            }
            appendLine(line)
            appendLine("TOTAL GAJI BERSIH (TAKE HOME PAY):")
            appendLine("   ${rupiahFormat.format(payroll.netSalary)}")
            appendLine(line)
            appendLine("Status: ${if (payroll.isPaid) "LUNAS / DIBAYARKAN" else "TERVERIFIKASI SISTEM"}")
            appendLine("")
            appendLine("   Penerima,            Manager,   ")
            appendLine("")
            appendLine("")
            appendLine("(${payroll.staffName})       (Store Manager)")
            appendLine(dash)
            appendLine("   Dicetak dari Dakota POS Engine")
        }
    }
}
