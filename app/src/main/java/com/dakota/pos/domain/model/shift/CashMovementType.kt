package com.dakota.pos.domain.model.shift

enum class CashMovementType {
    CASH_IN,     // Setoran kas tambahan / modal darurat
    CASH_OUT,    // Pengambilan kas (misal disetor ke brankas/owner)
    EXPENSE,     // Pengeluaran operasional kecil / petty cash (es batu, gas, galon)
    REFUND       // Pengembalian uang tunai ke pelanggan
}
