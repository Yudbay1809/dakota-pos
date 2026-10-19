package com.dakota.pos.domain.model.shift

data class CashMovement(
    val id: String,
    val shiftId: String,
    val type: CashMovementType,
    val amount: Double,
    val reason: String,
    val cashierId: String,
    val cashierName: String,
    val timestamp: Long = System.currentTimeMillis()
)
