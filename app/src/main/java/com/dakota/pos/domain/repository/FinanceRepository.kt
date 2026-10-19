package com.dakota.pos.domain.repository

import com.dakota.pos.domain.model.finance.OperatingExpense
import kotlinx.coroutines.flow.Flow

interface FinanceRepository {
    suspend fun saveExpense(expense: OperatingExpense)
    suspend fun getExpenseById(id: String): OperatingExpense?
    suspend fun deleteExpense(id: String)
    suspend fun getExpensesBetween(startTime: Long, endTime: Long): List<OperatingExpense>
    fun getExpensesBetweenFlow(startTime: Long, endTime: Long): Flow<List<OperatingExpense>>
    fun getAllExpensesFlow(): Flow<List<OperatingExpense>>
    suspend fun getTotalExpensesBetween(startTime: Long, endTime: Long): Double
}
