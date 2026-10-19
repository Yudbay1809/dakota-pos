package com.dakota.pos.domain.model.shift

data class CashierShift(
    val id: String,
    val cashierId: String,
    val cashierName: String,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val status: ShiftStatus = ShiftStatus.OPEN,
    val openingCash: Double, // Modal awal laci kasir
    val cashSales: Double = 0.0, // Total penjualan tunai
    val nonCashSales: Double = 0.0, // Non-tunai (QRIS, Card, Transfer)
    val totalCashIn: Double = 0.0, // Setoran tambahan
    val totalCashOut: Double = 0.0, // Pengambilan uang kasir
    val totalExpense: Double = 0.0, // Pengeluaran operasional (petty cash)
    val totalRefund: Double = 0.0, // Pengembalian tunai
    val expectedCash: Double = 0.0, // Hasil kalkulasi sistem
    val actualCash: Double? = null, // Uang fisik dihitung kasir saat tutup shift (blind count)
    val variance: Double? = null, // actualCash - expectedCash
    val closingNotes: String? = null,
    val totalTransactions: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Menghitung uang kas yang seharusnya ada di laci kasir berdasarkan rumus resmi:
     * Expected Cash = Opening Cash + Cash Sales + Cash In - Cash Out - Expense - Cash Refund
     */
    fun calculateExpectedCash(): Double {
        return openingCash + cashSales + totalCashIn - totalCashOut - totalExpense - totalRefund
    }

    /**
     * Menghitung selisih kas (Variance = Actual Cash - Expected Cash)
     */
    fun calculateVariance(physicalCash: Double): Double {
        return physicalCash - calculateExpectedCash()
    }

    val isBalanced: Boolean
        get() = (variance ?: 0.0) == 0.0

    val isOver: Boolean
        get() = (variance ?: 0.0) > 0.0

    val isShort: Boolean
        get() = (variance ?: 0.0) < 0.0
}
