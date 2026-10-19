package com.dakota.pos.domain.usecase.shift

import com.dakota.pos.domain.model.shift.CashMovement
import com.dakota.pos.domain.model.shift.CashMovementType
import com.dakota.pos.domain.model.shift.ShiftStatus
import com.dakota.pos.domain.repository.ShiftRepository
import java.util.UUID

class RecordCashMovementUseCase(
    private val shiftRepository: ShiftRepository
) {

    suspend operator fun invoke(
        shiftId: String,
        type: CashMovementType,
        amount: Double,
        reason: String,
        cashierId: String,
        cashierName: String
    ): Result<CashMovement> {
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Nominal kas masuk/keluar harus lebih besar dari 0"))
        }

        if (reason.isBlank()) {
            return Result.failure(IllegalArgumentException("Alasan / keterangan kas masuk/keluar wajib diisi"))
        }

        val shift = shiftRepository.getShiftById(shiftId)
            ?: return Result.failure(IllegalArgumentException("Shift dengan ID $shiftId tidak ditemukan"))

        if (shift.status != ShiftStatus.OPEN) {
            return Result.failure(IllegalStateException("Tidak dapat mencatat mutasi kas pada shift yang sudah ditutup (CLOSED)"))
        }

        val movementId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val movement = CashMovement(
            id = movementId,
            shiftId = shift.id,
            type = type,
            amount = amount,
            reason = reason.trim(),
            cashierId = cashierId,
            cashierName = cashierName,
            timestamp = now
        )

        // Simpan mutasi
        shiftRepository.recordCashMovement(movement)

        // Update akumulasi shift
        val updatedShift = when (type) {
            CashMovementType.CASH_IN -> shift.copy(
                totalCashIn = shift.totalCashIn + amount
            )
            CashMovementType.CASH_OUT -> shift.copy(
                totalCashOut = shift.totalCashOut + amount
            )
            CashMovementType.EXPENSE -> shift.copy(
                totalExpense = shift.totalExpense + amount
            )
            CashMovementType.REFUND -> shift.copy(
                totalRefund = shift.totalRefund + amount
            )
        }

        val finalShift = updatedShift.copy(
            expectedCash = updatedShift.calculateExpectedCash()
        )
        shiftRepository.saveShift(finalShift)

        return Result.success(movement)
    }
}
