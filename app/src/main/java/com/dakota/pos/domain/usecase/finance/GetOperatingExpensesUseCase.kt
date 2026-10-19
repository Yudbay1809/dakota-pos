package com.dakota.pos.domain.usecase.finance

import com.dakota.pos.domain.model.finance.OperatingExpense
import com.dakota.pos.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow

class GetOperatingExpensesUseCase(
    private val financeRepository: FinanceRepository
) {
    operator fun invoke(startTime: Long, endTime: Long): Flow<List<OperatingExpense>> {
        return financeRepository.getExpensesBetweenFlow(startTime, endTime)
    }

    fun getAllExpensesFlow(): Flow<List<OperatingExpense>> {
        return financeRepository.getAllExpensesFlow()
    }

    suspend fun getExpenses(startTime: Long, endTime: Long): List<OperatingExpense> {
        return financeRepository.getExpensesBetween(startTime, endTime)
    }
}
