package com.dakota.pos

import com.dakota.pos.domain.model.shift.CashMovement
import com.dakota.pos.domain.model.shift.CashMovementType
import com.dakota.pos.domain.model.shift.CashierShift
import com.dakota.pos.domain.model.shift.ShiftStatus
import com.dakota.pos.domain.repository.ShiftRepository
import com.dakota.pos.domain.usecase.shift.CloseShiftUseCase
import com.dakota.pos.domain.usecase.shift.GenerateShiftReportUseCase
import com.dakota.pos.domain.usecase.shift.GetCurrentShiftUseCase
import com.dakota.pos.domain.usecase.shift.OpenShiftUseCase
import com.dakota.pos.domain.usecase.shift.RecordCashMovementUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive Unit Tests for Phase 10: Shift + Cash Management.
 *
 * Test coverage:
 * 1. Open shift with valid opening cash float
 * 2. Validation: Negative opening cash rejection
 * 3. Validation: Prevention of opening duplicate concurrent shift
 * 4. Record Cash In (setoran kas tambahan)
 * 5. Record Cash Out (pengambilan kas)
 * 6. Record Expense (petty cash belanja darurat)
 * 7. Record Refund (pengembalian tunai)
 * 8. Validation: Reject cash movement on CLOSED shift
 * 9. Validation: Reject invalid amount or empty reason
 * 10. Close shift: Balanced variance (Expected Cash == Actual Cash)
 * 11. Close shift: Over variance (Actual Cash > Expected Cash)
 * 12. Close shift: Short variance (Actual Cash < Expected Cash)
 * 13. Validation: Reject negative actual cash input
 * 14. Validation: Reject closing an already closed shift
 * 15. Report generation: Mid-shift X-Report format
 * 16. Report generation: End-of-shift Z-Report format
 */
class ShiftManagementTest {

    private lateinit var fakeShiftRepo: FakeShiftRepository
    private lateinit var generateShiftReportUseCase: GenerateShiftReportUseCase
    private lateinit var openShiftUseCase: OpenShiftUseCase
    private lateinit var recordCashMovementUseCase: RecordCashMovementUseCase
    private lateinit var closeShiftUseCase: CloseShiftUseCase
    private lateinit var getCurrentShiftUseCase: GetCurrentShiftUseCase

    @Before
    fun setup() {
        fakeShiftRepo = FakeShiftRepository()
        generateShiftReportUseCase = GenerateShiftReportUseCase()
        openShiftUseCase = OpenShiftUseCase(fakeShiftRepo)
        recordCashMovementUseCase = RecordCashMovementUseCase(fakeShiftRepo)
        closeShiftUseCase = CloseShiftUseCase(fakeShiftRepo, generateShiftReportUseCase)
        getCurrentShiftUseCase = GetCurrentShiftUseCase(fakeShiftRepo)
    }

    // ========================================================================
    // 1. OPEN SHIFT TESTS
    // ========================================================================

    @Test
    fun testOpenShiftSuccess() = runBlocking {
        val result = openShiftUseCase(
            cashierId = "cashier-101",
            cashierName = "Budi Santoso",
            openingCash = 200000.0
        )

        assertTrue(result.isSuccess)
        val shift = result.getOrThrow()
        assertEquals("cashier-101", shift.cashierId)
        assertEquals("Budi Santoso", shift.cashierName)
        assertEquals(ShiftStatus.OPEN, shift.status)
        assertEquals(200000.0, shift.openingCash, 0.001)
        assertEquals(200000.0, shift.expectedCash, 0.001)
        assertNull(shift.endTime)
    }

    @Test
    fun testOpenShiftNegativeOpeningCashFails() = runBlocking {
        val result = openShiftUseCase(
            cashierId = "cashier-101",
            cashierName = "Budi Santoso",
            openingCash = -50000.0
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("tidak boleh bernilai negatif") == true)
    }

    @Test
    fun testOpenShiftPreventConcurrentDuplicateShift() = runBlocking {
        // Open first shift
        openShiftUseCase("cashier-101", "Budi", 200000.0)

        // Try open second shift while first is still OPEN
        val secondResult = openShiftUseCase("cashier-101", "Budi", 150000.0)
        assertTrue(secondResult.isFailure)
        assertTrue(secondResult.exceptionOrNull()?.message?.contains("masih aktif") == true)
    }

    // ========================================================================
    // 2. CASH MOVEMENTS TESTS (IN, OUT, EXPENSE, REFUND)
    // ========================================================================

    @Test
    fun testRecordCashIn() = runBlocking {
        val shift = openShiftUseCase("cashier-1", "Siti", 200000.0).getOrThrow()

        val moveResult = recordCashMovementUseCase(
            shiftId = shift.id,
            type = CashMovementType.CASH_IN,
            amount = 100000.0,
            reason = "Tambahan uang kembalian pecahan kecil",
            cashierId = "cashier-1",
            cashierName = "Siti"
        )

        assertTrue(moveResult.isSuccess)
        val movement = moveResult.getOrThrow()
        assertEquals(CashMovementType.CASH_IN, movement.type)
        assertEquals(100000.0, movement.amount, 0.001)

        val updatedShift = fakeShiftRepo.getShiftById(shift.id)!!
        assertEquals(100000.0, updatedShift.totalCashIn, 0.001)
        // Expected Cash = 200.000 + 100.000 = 300.000
        assertEquals(300000.0, updatedShift.expectedCash, 0.001)
    }

    @Test
    fun testRecordExpensePettyCash() = runBlocking {
        val shift = openShiftUseCase("cashier-1", "Siti", 200000.0).getOrThrow()

        val moveResult = recordCashMovementUseCase(
            shiftId = shift.id,
            type = CashMovementType.EXPENSE,
            amount = 25000.0,
            reason = "Beli es batu kristal 2 kantong",
            cashierId = "cashier-1",
            cashierName = "Siti"
        )

        assertTrue(moveResult.isSuccess)
        val updatedShift = fakeShiftRepo.getShiftById(shift.id)!!
        assertEquals(25000.0, updatedShift.totalExpense, 0.001)
        // Expected Cash = 200.000 - 25.000 = 175.000
        assertEquals(175000.0, updatedShift.expectedCash, 0.001)
    }

    @Test
    fun testRecordCashOutAndRefund() = runBlocking {
        val shift = openShiftUseCase("cashier-1", "Siti", 500000.0).getOrThrow()

        // Cash Out: 100k
        recordCashMovementUseCase(shift.id, CashMovementType.CASH_OUT, 100000.0, "Setor ke brankas", "cashier-1", "Siti")
        // Refund: 35k
        recordCashMovementUseCase(shift.id, CashMovementType.REFUND, 35000.0, "Refund order batal", "cashier-1", "Siti")

        val updatedShift = fakeShiftRepo.getShiftById(shift.id)!!
        assertEquals(100000.0, updatedShift.totalCashOut, 0.001)
        assertEquals(35000.0, updatedShift.totalRefund, 0.001)
        // Expected = 500.000 - 100.000 - 35.000 = 365.000
        assertEquals(365000.0, updatedShift.expectedCash, 0.001)
    }

    @Test
    fun testRecordMovementOnClosedShiftFails() = runBlocking {
        val shift = openShiftUseCase("cashier-1", "Siti", 200000.0).getOrThrow()
        closeShiftUseCase(shift.id, 200000.0)

        val result = recordCashMovementUseCase(
            shiftId = shift.id,
            type = CashMovementType.EXPENSE,
            amount = 10000.0,
            reason = "Beli kopi",
            cashierId = "cashier-1",
            cashierName = "Siti"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("sudah ditutup") == true)
    }

    @Test
    fun testRecordMovementInvalidAmountOrReason() = runBlocking {
        val shift = openShiftUseCase("cashier-1", "Siti", 200000.0).getOrThrow()

        // Zero / negative amount
        val r1 = recordCashMovementUseCase(shift.id, CashMovementType.CASH_IN, 0.0, "Test", "c1", "u1")
        assertTrue(r1.isFailure)

        // Blank reason
        val r2 = recordCashMovementUseCase(shift.id, CashMovementType.CASH_IN, 50000.0, "   ", "c1", "u1")
        assertTrue(r2.isFailure)
    }

    // ========================================================================
    // 3. RECONCILIATION & VARIANCE FORMULA TESTS
    // ========================================================================

    @Test
    fun testCloseShiftBalancedVariance() = runBlocking {
        // Setup:
        // Opening: 200.000
        // Mock sales during shift: Cash Sales = 350.000 (7 orders), Non-cash = 150.000
        // Cash In: 50.000
        // Expense: 20.000
        // Refund: 10.000
        // Expected Cash = 200.000 + 350.000 + 50.000 - 20.000 - 10.000 = 570.000
        fakeShiftRepo.mockCashSales = 350000.0
        fakeShiftRepo.mockNonCashSales = 150000.0
        fakeShiftRepo.mockTransactionCount = 7

        val shift = openShiftUseCase("cashier-1", "Budi", 200000.0).getOrThrow()
        recordCashMovementUseCase(shift.id, CashMovementType.CASH_IN, 50000.0, "Modal tambahan", "c1", "Budi")
        recordCashMovementUseCase(shift.id, CashMovementType.EXPENSE, 20000.0, "Beli air galon", "c1", "Budi")
        recordCashMovementUseCase(shift.id, CashMovementType.REFUND, 10000.0, "Salah pesan", "c1", "Budi")

        // Kasir hitung uang fisik pas: 570.000
        val closeResult = closeShiftUseCase(shift.id, actualCash = 570000.0, closingNotes = "Shift lancar").getOrThrow()
        val closed = closeResult.closedShift

        assertEquals(ShiftStatus.CLOSED, closed.status)
        assertNotNull(closed.endTime)
        assertEquals(350000.0, closed.cashSales, 0.001)
        assertEquals(150000.0, closed.nonCashSales, 0.001)
        assertEquals(7, closed.totalTransactions)
        assertEquals(570000.0, closed.expectedCash, 0.001)
        assertEquals(570000.0, closed.actualCash!!, 0.001)

        // Variance: 0.0
        assertEquals(0.0, closed.variance!!, 0.001)
        assertTrue(closed.isBalanced)
        assertFalse(closed.isOver)
        assertFalse(closed.isShort)

        // Verify Z-Report generated
        assertTrue(closeResult.zReportText.contains("Z-REPORT"))
        assertTrue(closeResult.zReportText.contains("PAS / SEIMBANG (BALANCED)"))
    }

    @Test
    fun testCloseShiftOverVariance() = runBlocking {
        fakeShiftRepo.mockCashSales = 200000.0
        val shift = openShiftUseCase("cashier-1", "Budi", 100000.0).getOrThrow()
        // Expected Cash = 100.000 + 200.000 = 300.000

        // Kasir hitung uang fisik lebih: 315.000 (Selisih +15.000)
        val closeResult = closeShiftUseCase(
            shiftId = shift.id,
            actualCash = 315000.0,
            closingNotes = "Kelebihan uang tip dari pelanggan tidak dimasukkan toples"
        ).getOrThrow()
        val closed = closeResult.closedShift

        assertEquals(300000.0, closed.expectedCash, 0.001)
        assertEquals(315000.0, closed.actualCash!!, 0.001)
        assertEquals(150000.0 - 135000.0, closed.variance!!, 0.001) // +15.000
        assertTrue(closed.isOver)
        assertFalse(closed.isShort)
        assertFalse(closed.isBalanced)
        assertTrue(closeResult.zReportText.contains("LEBIH (OVER)"))
    }

    @Test
    fun testCloseShiftShortVariance() = runBlocking {
        fakeShiftRepo.mockCashSales = 400000.0
        val shift = openShiftUseCase("cashier-1", "Budi", 100000.0).getOrThrow()
        // Expected Cash = 100.000 + 400.000 = 500.000

        // Kasir hitung uang fisik kurang: 480.000 (Selisih -20.000)
        val closeResult = closeShiftUseCase(
            shiftId = shift.id,
            actualCash = 480000.0,
            closingNotes = "Salah hitung kembalian pelanggan pagi"
        ).getOrThrow()
        val closed = closeResult.closedShift

        assertEquals(500000.0, closed.expectedCash, 0.001)
        assertEquals(480000.0, closed.actualCash!!, 0.001)
        assertEquals(-20000.0, closed.variance!!, 0.001)
        assertTrue(closed.isShort)
        assertFalse(closed.isOver)
        assertFalse(closed.isBalanced)
        assertTrue(closeResult.zReportText.contains("KURANG (SHORT)"))
    }

    @Test
    fun testCloseShiftValidation() = runBlocking {
        val shift = openShiftUseCase("cashier-1", "Budi", 100000.0).getOrThrow()

        // Negative actual cash
        val r1 = closeShiftUseCase(shift.id, actualCash = -10.0)
        assertTrue(r1.isFailure)

        // Close successfully
        val r2 = closeShiftUseCase(shift.id, actualCash = 100000.0)
        assertTrue(r2.isSuccess)

        // Close again on already closed shift
        val r3 = closeShiftUseCase(shift.id, actualCash = 100000.0)
        assertTrue(r3.isFailure)
        assertTrue(r3.exceptionOrNull()?.message?.contains("sudah ditutup") == true)
    }

    @Test
    fun testGenerateXReportMidShift() = runBlocking {
        fakeShiftRepo.mockCashSales = 150000.0
        val shift = openShiftUseCase("cashier-1", "Budi", 100000.0).getOrThrow()
        recordCashMovementUseCase(shift.id, CashMovementType.EXPENSE, 10000.0, "Es batu", "c1", "Budi")

        val currentMovements = fakeShiftRepo.getMovementsForShift(shift.id)
        val updated = getCurrentShiftUseCase.refreshShiftRealtimeSales(shift)
        val xReport = generateShiftReportUseCase.generateXReport(updated, currentMovements)

        assertTrue(xReport.contains("X-REPORT (MID-SHIFT AUDIT)"))
        assertTrue(xReport.contains("Status      : AKTIF (BELUM DITUTUP)"))
        assertTrue(xReport.contains("Es batu"))
        // Still open
        assertEquals(ShiftStatus.OPEN, fakeShiftRepo.getShiftById(shift.id)!!.status)
    }
}

// ============================================================================
// FAKE REPOSITORY FOR TESTING
// ============================================================================

class FakeShiftRepository : ShiftRepository {
    val shiftsMap = mutableMapOf<String, CashierShift>()
    val movementsMap = mutableMapOf<String, MutableList<CashMovement>>()

    var mockCashSales: Double = 0.0
    var mockNonCashSales: Double = 0.0
    var mockTransactionCount: Int = 0

    override suspend fun saveShift(shift: CashierShift) {
        shiftsMap[shift.id] = shift
    }

    override suspend fun getShiftById(id: String): CashierShift? = shiftsMap[id]

    override suspend fun getActiveShiftForCashier(cashierId: String): CashierShift? {
        return shiftsMap.values.find { it.cashierId == cashierId && it.status == ShiftStatus.OPEN }
    }

    override suspend fun getAnyActiveShift(): CashierShift? {
        return shiftsMap.values.find { it.status == ShiftStatus.OPEN }
    }

    override fun getActiveShiftFlow(): Flow<CashierShift?> {
        return flowOf(shiftsMap.values.find { it.status == ShiftStatus.OPEN })
    }

    override fun getAllShiftsFlow(): Flow<List<CashierShift>> {
        return flowOf(shiftsMap.values.sortedByDescending { it.startTime })
    }

    override fun getShiftsByCashierFlow(cashierId: String): Flow<List<CashierShift>> {
        return flowOf(shiftsMap.values.filter { it.cashierId == cashierId }.sortedByDescending { it.startTime })
    }

    override suspend fun recordCashMovement(movement: CashMovement) {
        val list = movementsMap.getOrPut(movement.shiftId) { mutableListOf() }
        list.add(movement)
    }

    override fun getMovementsForShiftFlow(shiftId: String): Flow<List<CashMovement>> {
        return flowOf(movementsMap[shiftId] ?: emptyList())
    }

    override suspend fun getMovementsForShift(shiftId: String): List<CashMovement> {
        return movementsMap[shiftId] ?: emptyList()
    }

    override suspend fun getCashSalesForPeriod(startTime: Long, endTime: Long): Double = mockCashSales

    override suspend fun getNonCashSalesForPeriod(startTime: Long, endTime: Long): Double = mockNonCashSales

    override suspend fun getTransactionCountForPeriod(startTime: Long, endTime: Long): Int = mockTransactionCount
}
