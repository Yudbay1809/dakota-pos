package com.dakota.pos.domain.model.finance

data class OperatingExpense(
    val id: String,
    val category: ExpenseCategory,
    val amount: Double,
    val description: String,
    val date: Long = System.currentTimeMillis(),
    val recordedBy: String,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
