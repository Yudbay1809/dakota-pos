package com.dakota.pos.domain.model.finance

enum class ExpenseCategory(val displayName: String) {
    RENT("Sewa Tempat"),
    ELECTRICITY("Listrik"),
    WATER("Air PDAM / Galon"),
    INTERNET("Internet & WiFi"),
    SALARY("Gaji & Insentif Karyawan"),
    MARKETING("Pemasaran & Promosi"),
    MAINTENANCE("Servis Mesin & Perawatan"),
    CLEANING("Sanitasi & Kebersihan"),
    OTHER("Biaya Lain-lain");

    companion object {
        fun fromString(value: String): ExpenseCategory {
            return entries.find { it.name.equals(value, ignoreCase = true) }
                ?: entries.find { it.displayName.equals(value, ignoreCase = true) }
                ?: OTHER
        }
    }
}
