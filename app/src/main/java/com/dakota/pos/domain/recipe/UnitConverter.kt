package com.dakota.pos.domain.recipe

import com.dakota.pos.domain.model.unit.UnitDimension
import com.dakota.pos.domain.model.unit.UnitType

/**
 * Engine konversi unit pengukuran untuk bahan baku dan resep coffee shop.
 * Memastikan konversi hanya terjadi pada dimensi yang kompatibel (Weight ↔ Weight, Volume ↔ Volume, Count ↔ Count).
 */
object UnitConverter {

    /**
     * Memeriksa apakah dua unit memiliki dimensi yang sama dan dapat dikonversi.
     */
    fun areCompatible(from: UnitType, to: UnitType): Boolean {
        return from.dimension == to.dimension
    }

    /**
     * Mengonversi kuantitas dari satu satuan ke satuan lain.
     * Contoh:
     * - convert(1.5, KG, G) -> 1500.0 g
     * - convert(250.0, ML, L) -> 0.25 L
     * @throws IncompatibleUnitException jika dimensi berbeda (misal: Gram ke Liter tanpa densitas)
     */
    fun convert(amount: Double, from: UnitType, to: UnitType): Double {
        if (from == to) return amount

        if (!areCompatible(from, to)) {
            throw IncompatibleUnitException(
                "Tidak dapat mengonversi satuan ${from.symbol} (${from.dimension}) ke ${to.symbol} (${to.dimension}): dimensi tidak kompatibel."
            )
        }

        // Konversi ke base unit terlebih dahulu, lalu ke target unit
        val baseAmount = amount * from.baseMultiplier
        return baseAmount / to.baseMultiplier
    }

    /**
     * Menghitung biaya per target unit berdasarkan biaya per original unit.
     * Contoh:
     * - Biji kopi Rp 250.000 / kg -> untuk unit 'g' = Rp 250 / g
     * - Susu Rp 20.000 / L -> untuk unit 'ml' = Rp 20 / ml
     */
    fun convertCost(costPerOriginalUnit: Double, originalUnit: UnitType, targetUnit: UnitType): Double {
        if (originalUnit == targetUnit) return costPerOriginalUnit

        if (!areCompatible(originalUnit, targetUnit)) {
            throw IncompatibleUnitException(
                "Tidak dapat menghitung biaya: satuan ${originalUnit.symbol} tidak kompatibel dengan ${targetUnit.symbol}."
            )
        }

        // 1 originalUnit = X targetUnit
        val ratio = convert(1.0, originalUnit, targetUnit)
        if (ratio == 0.0) return 0.0

        return costPerOriginalUnit / ratio
    }
}

class IncompatibleUnitException(message: String) : IllegalArgumentException(message)
