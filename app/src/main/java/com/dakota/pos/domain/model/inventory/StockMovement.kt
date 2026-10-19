package com.dakota.pos.domain.model.inventory

import com.dakota.pos.domain.model.unit.UnitType

enum class MovementType {
    IN,          // Stok masuk manual / penerimaan
    OUT,         // Stok keluar manual
    SALE,        // Pengurangan otomatis dari transaksi penjualan POS
    ADJUSTMENT,  // Penyesuaian stok dari hasil stock opname
    WASTAGE,     // Terbuang / tumpah / basi / kalibrasi grinder
    RETURN       // Pengembalian stok akibat pembatalan order / refund
}

data class StockMovement(
    val id: String,
    val ingredientId: String,
    val ingredientName: String,
    val type: MovementType,
    val quantityChange: Double, // Positif jika bertambah, negatif jika berkurang
    val unit: UnitType,
    val balanceBefore: Double,
    val balanceAfter: Double,
    val referenceId: String? = null, // e.g. orderId, opnameId
    val notes: String? = null,
    val userId: String,
    val userName: String,
    val timestamp: Long = System.currentTimeMillis()
)
