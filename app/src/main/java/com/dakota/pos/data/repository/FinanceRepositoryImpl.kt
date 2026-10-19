package com.dakota.pos.data.repository

import com.dakota.pos.data.local.dao.OperatingExpenseDao
import com.dakota.pos.data.local.entity.OperatingExpenseEntity
import com.dakota.pos.domain.model.finance.ExpenseCategory
import com.dakota.pos.domain.model.finance.OperatingExpense
import com.dakota.pos.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FinanceRepositoryImpl(
    private val operatingExpenseDao: OperatingExpenseDao
) : FinanceRepository {

    override suspend fun saveExpense(expense: OperatingExpense) {
        operatingExpenseDao.insertExpense(expense.toEntity())
    }

    override suspend fun getExpenseById(id: String): OperatingExpense? {
        return operatingExpenseDao.getExpenseById(id)?.toDomain()
    }

    override suspend fun deleteExpense(id: String) {
        operatingExpenseDao.deleteExpenseById(id)
    }

    override suspend fun getExpensesBetween(startTime: Long, endTime: Long): List<OperatingExpense> {
        return operatingExpenseDao.getExpensesBetween(startTime, endTime).map { it.toDomain() }
    }

    override fun getExpensesBetweenFlow(startTime: Long, endTime: Long): Flow<List<OperatingExpense>> {
        return operatingExpenseDao.getExpensesBetweenFlow(startTime, endTime).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllExpensesFlow(): Flow<List<OperatingExpense>> {
        return operatingExpenseDao.getAllExpensesFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTotalExpensesBetween(startTime: Long, endTime: Long): Double {
        return operatingExpenseDao.getTotalExpenseBetween(startTime, endTime) ?: 0.0
    }

    // ========================================================================
    // MAPPERS
    // ========================================================================

    private fun OperatingExpense.toEntity() = OperatingExpenseEntity(
        id = id,
        category = category.name,
        amount = amount,
        description = description,
        date = date,
        recordedBy = recordedBy,
        notes = notes,
        createdAt = createdAt
    )

    private fun OperatingExpenseEntity.toDomain() = OperatingExpense(
        id = id,
        category = ExpenseCategory.fromString(category),
        amount = amount,
        description = description,
        date = date,
        recordedBy = recordedBy,
        notes = notes,
        createdAt = createdAt
    )
}
