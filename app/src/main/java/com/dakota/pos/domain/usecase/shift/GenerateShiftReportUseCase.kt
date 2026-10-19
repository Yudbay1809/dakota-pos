package com.dakota.pos.domain.usecase.shift

import com.dakota.pos.domain.model.shift.CashMovement
import com.dakota.pos.domain.model.shift.CashierShift
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GenerateShiftReportUseCase {

    private val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    private fun formatRupiah(amount: Double): String = rupiahFormat.format(amount)

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID"))
        return sdf.format(Date(timestamp))
    }

    /**
     * Menghasilkan teks laporan X-Report (Audit kas berjalan di tengah shift)
     */
    fun generateXReport(shift: CashierShift, movements: List<CashMovement>): String {
        return buildReport(
            title = "X-REPORT (MID-SHIFT AUDIT)",
            shift = shift,
            movements = movements,
            isClosingReport = false
        )
    }

    /**
     * Menghasilkan teks laporan Z-Report (Laporan resmi penutupan shift dan rekonsiliasi kas)
     */
    fun generateZReport(shift: CashierShift, movements: List<CashMovement>): String {
        return buildReport(
            title = "Z-REPORT (END-OF-SHIFT CLOSING)",
            shift = shift,
            movements = movements,
            isClosingReport = true
        )
    }

    private fun buildReport(
        title: String,
        shift: CashierShift,
        movements: List<CashMovement>,
        isClosingReport: Boolean
    ): String {
        val sb = StringBuilder()
        val sep = "=".repeat(40)
        val dash = "-".repeat(40)

        sb.appendLine(sep)
        sb.appendLine("       DAKOTA COFFEE SHOP")
        sb.appendLine("    LAPORAN KASIR & REKONSILIASI")
        sb.appendLine(sep)
        sb.appendLine(title)
        sb.appendLine("Shift ID    : ${shift.id.take(8)}")
        sb.appendLine("Kasir       : ${shift.cashierName} (${shift.cashierId})")
        sb.appendLine("Buka Shift  : ${formatDate(shift.startTime)}")
        if (shift.endTime != null) {
            sb.appendLine("Tutup Shift : ${formatDate(shift.endTime)}")
        } else {
            sb.appendLine("Waktu Cetak : ${formatDate(System.currentTimeMillis())}")
            sb.appendLine("Status      : AKTIF (BELUM DITUTUP)")
        }
        sb.appendLine(dash)

        sb.appendLine("RINGKASAN PENJUALAN:")
        sb.appendLine("Total Transaksi : ${shift.totalTransactions}")
        sb.appendLine("Penjualan Tunai : ${formatRupiah(shift.cashSales)}")
        sb.appendLine("Penjualan Non-Tunai: ${formatRupiah(shift.nonCashSales)}")
        val totalRevenue = shift.cashSales + shift.nonCashSales
        sb.appendLine("Total Penjualan : ${formatRupiah(totalRevenue)}")
        sb.appendLine(dash)

        sb.appendLine("MUTASI KAS LACI (CASH DRAWER):")
        sb.appendLine("Modal Awal (Opening) : ${formatRupiah(shift.openingCash)}")
        sb.appendLine("(+) Penjualan Tunai  : ${formatRupiah(shift.cashSales)}")
        sb.appendLine("(+) Kas Masuk        : ${formatRupiah(shift.totalCashIn)}")
        sb.appendLine("(-) Kas Keluar       : ${formatRupiah(shift.totalCashOut)}")
        sb.appendLine("(-) Petty Cash/Biaya : ${formatRupiah(shift.totalExpense)}")
        sb.appendLine("(-) Refund Tunai     : ${formatRupiah(shift.totalRefund)}")
        sb.appendLine(dash)

        val expected = shift.calculateExpectedCash()
        sb.appendLine("Ekspektasi Kas Sistem: ${formatRupiah(expected)}")

        if (isClosingReport && shift.actualCash != null) {
            sb.appendLine("Uang Fisik Kasir (Aktual): ${formatRupiah(shift.actualCash)}")
            val variance = shift.variance ?: (shift.actualCash - expected)
            val varianceStatus = when {
                variance > 0 -> "LEBIH (OVER) (+${formatRupiah(variance)})"
                variance < 0 -> "KURANG (SHORT) (${formatRupiah(variance)})"
                else -> "PAS / SEIMBANG (BALANCED)"
            }
            sb.appendLine("Selisih Kas (Variance)  : $varianceStatus")

            if (!shift.closingNotes.isNullOrBlank()) {
                sb.appendLine("Catatan Kasir: ${shift.closingNotes}")
            }
        }

        if (movements.isNotEmpty()) {
            sb.appendLine(dash)
            sb.appendLine("RINCIAN MUTASI KAS:")
            movements.forEach { m ->
                val sign = if (m.type.name.startsWith("CASH_IN")) "+" else "-"
                sb.appendLine("${m.type.name} : $sign${formatRupiah(m.amount)}")
                sb.appendLine("  Ket: ${m.reason} (${formatDate(m.timestamp)})")
            }
        }

        sb.appendLine(sep)
        sb.appendLine(" Tanda Tangan Kasir     Tanda Tangan Spv")
        sb.appendLine("\n\n")
        sb.appendLine("(__________________)   (__________________) ")
        sb.appendLine(sep)

        return sb.toString()
    }
}
