package com.dakota.pos.domain.repository

import com.dakota.pos.domain.model.shift.CashMovement
import com.dakota.pos.domain.model.shift.CashierShift
import kotlinx.coroutines.flow.Flow

interface ShiftRepository {

    // Shifts
    suspend fun saveShift(shift: CashierShift)
    suspend fun getShiftById(id: String): CashierShift?
    suspend fun getActiveShiftForCashier(cashierId: String): CashierShift?
    suspend fun getAnyActiveShift(): CashierShift?
    fun getActiveShiftFlow(): Flow<CashierShift?>
    fun getAllShiftsFlow(): Flow<List<CashierShift>>
    fun getShiftsByCashierFlow(cashierId: String): Flow<List<CashierShift>>

    // Cash Movements
    suspend fun recordCashMovement(movement: CashMovement)
    fun getMovementsForShiftFlow(shiftId: String): Flow<List<CashMovement>>
    suspend fun getMovementsForShift(shiftId: String): List<CashMovement>

    // Sales Aggregation for Shift
    suspend fun getCashSalesForPeriod(startTime: Long, endTime: Long): Double
    suspend fun getNonCashSalesForPeriod(startTime: Long, endTime: Long): Double
    suspend fun getTransactionCountForPeriod(startTime: Long, endTime: Long): Int
}
