package com.dakota.pos.ui.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dakota.pos.domain.model.finance.ExpenseCategory
import com.dakota.pos.domain.model.finance.ProfitLossReport
import com.dakota.pos.domain.usecase.finance.DeleteOperatingExpenseUseCase
import com.dakota.pos.domain.usecase.finance.GenerateProfitLossReportUseCase
import com.dakota.pos.domain.usecase.finance.GetOperatingExpensesUseCase
import com.dakota.pos.domain.usecase.finance.RecordOperatingExpenseUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ReportPeriod(val displayName: String) {
    TODAY("Hari Ini"),
    LAST_7_DAYS("7 Hari"),
    THIS_MONTH("Bulan Ini"),
    LAST_MONTH("Bulan Lalu"),
    ALL_TIME("Semua")
}

data class FinanceUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.TODAY,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val report: ProfitLossReport? = null,
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val showAddExpenseDialog: Boolean = false,
    val activeTab: Int = 0 // 0: Overview P&L, 1: Operating Expenses, 2: Menu Profitability
)

class FinanceViewModel(
    private val generateProfitLossReportUseCase: GenerateProfitLossReportUseCase,
    private val recordOperatingExpenseUseCase: RecordOperatingExpenseUseCase,
    private val deleteOperatingExpenseUseCase: DeleteOperatingExpenseUseCase,
    private val getOperatingExpensesUseCase: GetOperatingExpensesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    init {
        loadReport()
    }

    fun selectPeriod(period: ReportPeriod) {
        _uiState.update { it.copy(selectedPeriod = period, customStartDate = null, customEndDate = null) }
        loadReport()
    }

    fun setTab(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun openAddExpenseDialog() {
        _uiState.update { it.copy(showAddExpenseDialog = true) }
    }

    fun closeAddExpenseDialog() {
        _uiState.update { it.copy(showAddExpenseDialog = false) }
    }

    fun loadReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val (start, end) = getTimestampsForPeriod(_uiState.value.selectedPeriod)
            val reportResult = runCatching {
                generateProfitLossReportUseCase(start, end)
            }

            reportResult.fold(
                onSuccess = { report ->
                    _uiState.update { it.copy(report = report, isLoading = false) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = err.message ?: "Gagal memuat laporan keuangan"
                        )
                    }
                }
            )
        }
    }

    fun recordExpense(
        category: ExpenseCategory,
        amount: Double,
        description: String,
        recordedBy: String,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = recordOperatingExpenseUseCase(
                category = category,
                amount = amount,
                description = description,
                recordedBy = recordedBy,
                notes = notes
            )

            result.fold(
                onSuccess = { createdExpense ->
                    _uiState.update { state ->
                        state.copy(
                            showAddExpenseDialog = false,
                            message = "Biaya operasional '${createdExpense.description}' berhasil dicatat"
                        )
                    }
                    loadReport()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = err.message ?: "Gagal mencatat biaya operasional"
                        )
                    }
                }
            )
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = deleteOperatingExpenseUseCase(id)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(message = "Biaya operasional berhasil dihapus") }
                    loadReport()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = err.message ?: "Gagal menghapus biaya operasional"
                        )
                    }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    private fun getTimestampsForPeriod(period: ReportPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        return when (period) {
            ReportPeriod.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                start to now
            }
            ReportPeriod.LAST_7_DAYS -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                start to now
            }
            ReportPeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                start to now
            }
            ReportPeriod.LAST_MONTH -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                val endCal = Calendar.getInstance()
                endCal.set(Calendar.DAY_OF_MONTH, 1)
                endCal.add(Calendar.MILLISECOND, -1)
                val end = endCal.timeInMillis
                start to end
            }
            ReportPeriod.ALL_TIME -> {
                0L to now
            }
        }
    }
}
