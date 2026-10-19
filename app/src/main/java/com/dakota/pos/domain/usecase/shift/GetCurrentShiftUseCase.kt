package com.dakota.pos.domain.usecase.shift

import com.dakota.pos.domain.model.shift.CashMovement
import com.dakota.pos.domain.model.shift.CashierShift
import com.dakota.pos.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.Flow

class GetCurrentShiftUseCase(
    private val shiftRepository: ShiftRepository
) {

    fun getActiveShiftFlow(): Flow<CashierShift?> = shiftRepository.getActiveShiftFlow()

    suspend fun getActiveShift(cashierId: String? = null): CashierShift? {
        return if (cashierId != null) {
            shiftRepository.getActiveShiftForCashier(cashierId) ?: shiftRepository.getAnyActiveShift()
        } else {
            shiftRepository.getAnyActiveShift()
        }
    }

    fun getAllShiftsFlow(): Flow<List<CashierShift>> = shiftRepository.getAllShiftsFlow()

    fun getMovementsForShiftFlow(shiftId: String): Flow<List<CashMovement>> =
        shiftRepository.getMovementsForShiftFlow(shiftId)

    suspend fun getMovementsForShift(shiftId: String): List<CashMovement> =
        shiftRepository.getMovementsForShift(shiftId)

    suspend fun refreshShiftRealtimeSales(shift: CashierShift): CashierShift {
        val current = shiftRepository.getShiftById(shift.id) ?: shift
        val now = System.currentTimeMillis()
        val cashSales = shiftRepository.getCashSalesForPeriod(current.startTime, now)
        val nonCashSales = shiftRepository.getNonCashSalesForPeriod(current.startTime, now)
        val count = shiftRepository.getTransactionCountForPeriod(current.startTime, now)

        val updated = current.copy(
            cashSales = cashSales,
            nonCashSales = nonCashSales,
            totalTransactions = count,
            expectedCash = current.calculateExpectedCash()
        )
        shiftRepository.saveShift(updated)
        return updated
    }
}
