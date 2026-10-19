package com.dakota.pos.data.repository

import com.dakota.pos.data.local.dao.CashMovementDao
import com.dakota.pos.data.local.dao.CashierShiftDao
import com.dakota.pos.data.local.dao.OrderDao
import com.dakota.pos.data.local.entity.CashMovementEntity
import com.dakota.pos.data.local.entity.CashierShiftEntity
import com.dakota.pos.domain.model.shift.CashMovement
import com.dakota.pos.domain.model.shift.CashMovementType
import com.dakota.pos.domain.model.shift.CashierShift
import com.dakota.pos.domain.model.shift.ShiftStatus
import com.dakota.pos.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShiftRepositoryImpl(
    private val cashierShiftDao: CashierShiftDao,
    private val cashMovementDao: CashMovementDao,
    private val orderDao: OrderDao
) : ShiftRepository {

    override suspend fun saveShift(shift: CashierShift) {
        cashierShiftDao.insertShift(shift.toEntity())
    }

    override suspend fun getShiftById(id: String): CashierShift? {
        return cashierShiftDao.getShiftById(id)?.toDomain()
    }

    override suspend fun getActiveShiftForCashier(cashierId: String): CashierShift? {
        return cashierShiftDao.getActiveShiftForCashier(cashierId)?.toDomain()
    }

    override suspend fun getAnyActiveShift(): CashierShift? {
        return cashierShiftDao.getAnyActiveShift()?.toDomain()
    }

    override fun getActiveShiftFlow(): Flow<CashierShift?> {
        return cashierShiftDao.getActiveShiftFlow().map { it?.toDomain() }
    }

    override fun getAllShiftsFlow(): Flow<List<CashierShift>> {
        return cashierShiftDao.getAllShiftsFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getShiftsByCashierFlow(cashierId: String): Flow<List<CashierShift>> {
        return cashierShiftDao.getShiftsByCashierFlow(cashierId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun recordCashMovement(movement: CashMovement) {
        cashMovementDao.insertMovement(movement.toEntity())
    }

    override fun getMovementsForShiftFlow(shiftId: String): Flow<List<CashMovement>> {
        return cashMovementDao.getMovementsForShiftFlow(shiftId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getMovementsForShift(shiftId: String): List<CashMovement> {
        return cashMovementDao.getMovementsForShift(shiftId).map { it.toDomain() }
    }

    override suspend fun getCashSalesForPeriod(startTime: Long, endTime: Long): Double {
        return orderDao.getCashSalesForPeriod(startTime, endTime) ?: 0.0
    }

    override suspend fun getNonCashSalesForPeriod(startTime: Long, endTime: Long): Double {
        return orderDao.getNonCashSalesForPeriod(startTime, endTime) ?: 0.0
    }

    override suspend fun getTransactionCountForPeriod(startTime: Long, endTime: Long): Int {
        return orderDao.getTransactionCountForPeriod(startTime, endTime)
    }

    // ========================================================================
    // MAPPERS
    // ========================================================================

    private fun CashierShiftEntity.toDomain() = CashierShift(
        id = id,
        cashierId = cashierId,
        cashierName = cashierName,
        startTime = startTime,
        endTime = endTime,
        status = runCatching { ShiftStatus.valueOf(status) }.getOrDefault(ShiftStatus.OPEN),
        openingCash = openingCash,
        cashSales = cashSales,
        nonCashSales = nonCashSales,
        totalCashIn = totalCashIn,
        totalCashOut = totalCashOut,
        totalExpense = totalExpense,
        totalRefund = totalRefund,
        expectedCash = expectedCash,
        actualCash = actualCash,
        variance = variance,
        closingNotes = closingNotes,
        totalTransactions = totalTransactions,
        createdAt = createdAt
    )

    private fun CashierShift.toEntity() = CashierShiftEntity(
        id = id,
        cashierId = cashierId,
        cashierName = cashierName,
        startTime = startTime,
        endTime = endTime,
        status = status.name,
        openingCash = openingCash,
        cashSales = cashSales,
        nonCashSales = nonCashSales,
        totalCashIn = totalCashIn,
        totalCashOut = totalCashOut,
        totalExpense = totalExpense,
        totalRefund = totalRefund,
        expectedCash = expectedCash,
        actualCash = actualCash,
        variance = variance,
        closingNotes = closingNotes,
        totalTransactions = totalTransactions,
        createdAt = createdAt
    )

    private fun CashMovementEntity.toDomain() = CashMovement(
        id = id,
        shiftId = shiftId,
        type = runCatching { CashMovementType.valueOf(type) }.getOrDefault(CashMovementType.EXPENSE),
        amount = amount,
        reason = reason,
        cashierId = cashierId,
        cashierName = cashierName,
        timestamp = timestamp
    )

    private fun CashMovement.toEntity() = CashMovementEntity(
        id = id,
        shiftId = shiftId,
        type = type.name,
        amount = amount,
        reason = reason,
        cashierId = cashierId,
        cashierName = cashierName,
        timestamp = timestamp
    )
}
