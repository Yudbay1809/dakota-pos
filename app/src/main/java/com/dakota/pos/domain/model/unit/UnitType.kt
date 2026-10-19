package com.dakota.pos.domain.model.unit

enum class UnitDimension {
    WEIGHT,
    VOLUME,
    COUNT
}

enum class UnitType(
    val symbol: String,
    val displayName: String,
    val dimension: UnitDimension,
    val baseMultiplier: Double // multiplier to base unit (base: G for WEIGHT, ML for VOLUME, PCS for COUNT)
) {
    // Weight
    G("g", "Gram", UnitDimension.WEIGHT, 1.0),
    KG("kg", "Kilogram", UnitDimension.WEIGHT, 1000.0),

    // Volume
    ML("ml", "Mililiter", UnitDimension.VOLUME, 1.0),
    L("L", "Liter", UnitDimension.VOLUME, 1000.0),

    // Discrete
    PCS("pcs", "Pieces", UnitDimension.COUNT, 1.0);

    companion object {
        fun fromSymbol(symbol: String): UnitType {
            return entries.firstOrNull { it.symbol.equals(symbol.trim(), ignoreCase = true) }
                ?: entries.firstOrNull { it.name.equals(symbol.trim(), ignoreCase = true) }
                ?: PCS
        }

        fun fromString(str: String): UnitType = fromSymbol(str)
    }
}
