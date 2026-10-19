package com.dakota.pos.domain.usecase.shift

import com.dakota.pos.domain.model.shift.CashierShift
import com.dakota.pos.domain.model.shift.ShiftStatus
import com.dakota.pos.domain.repository.ShiftRepository
import java.util.UUID

class OpenShiftUseCase(
    private val shiftRepository: ShiftRepository
) {

    suspend operator fun invoke(
        cashierId: String,
        cashierName: String,
        openingCash: Double
    ): Result<CashierShift> {
        if (openingCash < 0.0) {
            return Result.failure(IllegalArgumentException("Modal awal kasir tidak boleh bernilai negatif"))
        }

        // Cek apakah kasir ini (atau laci kasir saat ini) masih memiliki shift aktif yang belum ditutup
        val existingShift = shiftRepository.getActiveShiftForCashier(cashierId)
            ?: shiftRepository.getAnyActiveShift()

        if (existingShift != null && existingShift.status == ShiftStatus.OPEN) {
            return Result.failure(
                IllegalStateException("Shift kasir '${existingShift.cashierName}' masih aktif (ID: ${existingShift.id}). Harap lakukan Tutup Shift terlebih dahulu sebelum membuka shift baru.")
            )
        }

        val shiftId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val newShift = CashierShift(
            id = shiftId,
            cashierId = cashierId,
            cashierName = cashierName,
            startTime = now,
            status = ShiftStatus.OPEN,
            openingCash = openingCash,
            expectedCash = openingCash,
            createdAt = now
        )

        shiftRepository.saveShift(newShift)
        return Result.success(newShift)
    }
}
