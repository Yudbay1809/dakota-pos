package com.dakota.pos.domain.usecase.finance

import com.dakota.pos.domain.model.finance.ExpenseCategory
import com.dakota.pos.domain.model.finance.OperatingExpense
import com.dakota.pos.domain.repository.FinanceRepository
import java.util.UUID

class RecordOperatingExpenseUseCase(
    private val financeRepository: FinanceRepository
) {
    suspend operator fun invoke(
        category: ExpenseCategory,
        amount: Double,
        description: String,
        recordedBy: String,
        notes: String? = null,
        date: Long = System.currentTimeMillis()
    ): Result<OperatingExpense> {
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Nominal biaya operasional harus lebih besar dari 0"))
        }
        if (description.isBlank()) {
            return Result.failure(IllegalArgumentException("Keterangan biaya operasional tidak boleh kosong"))
        }

        val expense = OperatingExpense(
            id = UUID.randomUUID().toString(),
            category = category,
            amount = amount,
            description = description.trim(),
            date = date,
            recordedBy = recordedBy.trim(),
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            createdAt = System.currentTimeMillis()
        )

        financeRepository.saveExpense(expense)
        return Result.success(expense)
    }
}
