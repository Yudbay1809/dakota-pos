package com.dakota.pos.domain.usecase.shift

import com.dakota.pos.domain.model.shift.CashMovementType
import com.dakota.pos.domain.model.shift.CashierShift
import com.dakota.pos.domain.model.shift.ShiftStatus
import com.dakota.pos.domain.repository.ShiftRepository

data class ShiftCloseResult(
    val closedShift: CashierShift,
    val zReportText: String
)

class CloseShiftUseCase(
    private val shiftRepository: ShiftRepository,
    private val generateShiftReportUseCase: GenerateShiftReportUseCase
) {

    suspend operator fun invoke(
        shiftId: String,
        actualCash: Double,
        closingNotes: String? = null
    ): Result<ShiftCloseResult> {
        if (actualCash < 0.0) {
            return Result.failure(IllegalArgumentException("Jumlah uang fisik aktual tidak boleh bernilai negatif"))
        }

        val shift = shiftRepository.getShiftById(shiftId)
            ?: return Result.failure(IllegalArgumentException("Shift dengan ID $shiftId tidak ditemukan"))

        if (shift.status != ShiftStatus.OPEN) {
            return Result.failure(IllegalStateException("Shift ini sudah ditutup sebelumnya (status: ${shift.status})"))
        }

        val now = System.currentTimeMillis()

        // Ambil penjualan riil dari database transaksi untuk periode shift
        val cashSales = shiftRepository.getCashSalesForPeriod(shift.startTime, now)
        val nonCashSales = shiftRepository.getNonCashSalesForPeriod(shift.startTime, now)
        val totalTransactions = shiftRepository.getTransactionCountForPeriod(shift.startTime, now)

        // Ambil mutasi kas untuk shift ini
        val movements = shiftRepository.getMovementsForShift(shift.id)
        val totalCashIn = movements.filter { it.type == CashMovementType.CASH_IN }.sumOf { it.amount }
        val totalCashOut = movements.filter { it.type == CashMovementType.CASH_OUT }.sumOf { it.amount }
        val totalExpense = movements.filter { it.type == CashMovementType.EXPENSE }.sumOf { it.amount }
        val totalRefund = movements.filter { it.type == CashMovementType.REFUND }.sumOf { it.amount }

        // Kalkulasi Expected Cash sesuai rumus resmi:
        // Expected Cash = Opening Cash + Cash Sales + Cash In - Cash Out - Expense - Refund
        val expectedCash = shift.openingCash + cashSales + totalCashIn - totalCashOut - totalExpense - totalRefund

        // Variance = Actual Cash - Expected Cash
        val variance = actualCash - expectedCash

        val closedShift = shift.copy(
            status = ShiftStatus.CLOSED,
            endTime = now,
            cashSales = cashSales,
            nonCashSales = nonCashSales,
            totalTransactions = totalTransactions,
            totalCashIn = totalCashIn,
            totalCashOut = totalCashOut,
            totalExpense = totalExpense,
            totalRefund = totalRefund,
            expectedCash = expectedCash,
            actualCash = actualCash,
            variance = variance,
            closingNotes = closingNotes?.trim()?.takeIf { it.isNotBlank() }
        )

        shiftRepository.saveShift(closedShift)

        // Generate Z-Report
        val zReport = generateShiftReportUseCase.generateZReport(closedShift, movements)

        return Result.success(
            ShiftCloseResult(
                closedShift = closedShift,
                zReportText = zReport
            )
        )
    }
}
