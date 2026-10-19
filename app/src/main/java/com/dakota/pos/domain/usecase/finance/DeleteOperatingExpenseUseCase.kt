package com.dakota.pos.domain.usecase.finance

import com.dakota.pos.domain.repository.FinanceRepository

class DeleteOperatingExpenseUseCase(
    private val financeRepository: FinanceRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return runCatching {
            financeRepository.deleteExpense(id)
        }
    }
}
