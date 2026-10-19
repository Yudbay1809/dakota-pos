package com.dakota.pos.ui.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dakota.pos.domain.model.shift.CashMovement
import com.dakota.pos.domain.model.shift.CashMovementType
import com.dakota.pos.domain.model.shift.CashierShift
import com.dakota.pos.domain.usecase.shift.CloseShiftUseCase
import com.dakota.pos.domain.usecase.shift.GenerateShiftReportUseCase
import com.dakota.pos.domain.usecase.shift.GetCurrentShiftUseCase
import com.dakota.pos.domain.usecase.shift.OpenShiftUseCase
import com.dakota.pos.domain.usecase.shift.RecordCashMovementUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShiftUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,

    // Dialog flags
    val showOpenShiftDialog: Boolean = false,
    val showCashMovementDialog: Boolean = false,
    val cashMovementType: CashMovementType = CashMovementType.EXPENSE,

    val showCloseShiftDialog: Boolean = false,
    val showReportPreviewDialog: Boolean = false,
    val reportPreviewTitle: String = "Laporan Shift",
    val reportPreviewText: String? = null
)

class ShiftViewModel(
    private val openShiftUseCase: OpenShiftUseCase,
    private val recordCashMovementUseCase: RecordCashMovementUseCase,
    private val closeShiftUseCase: CloseShiftUseCase,
    private val getCurrentShiftUseCase: GetCurrentShiftUseCase,
    private val generateShiftReportUseCase: GenerateShiftReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShiftUiState())
    val uiState: StateFlow<ShiftUiState> = _uiState.asStateFlow()

    val activeShift: StateFlow<CashierShift?> = getCurrentShiftUseCase.getActiveShiftFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allShifts: StateFlow<List<CashierShift>> = getCurrentShiftUseCase.getAllShiftsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _movements = MutableStateFlow<List<CashMovement>>(emptyList())
    val movements: StateFlow<List<CashMovement>> = _movements.asStateFlow()

    init {
        // Pantau mutasi untuk shift aktif
        viewModelScope.launch {
            activeShift.collect { shift ->
                if (shift != null) {
                    _movements.value = getCurrentShiftUseCase.getMovementsForShift(shift.id)
                } else {
                    _movements.value = emptyList()
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    // ========================================================================
    // OPEN SHIFT
    // ========================================================================

    fun openOpenShiftDialog() {
        _uiState.value = _uiState.value.copy(showOpenShiftDialog = true)
    }

    fun closeOpenShiftDialog() {
        _uiState.value = _uiState.value.copy(showOpenShiftDialog = false)
    }

    fun openShift(cashierId: String, cashierName: String, openingCash: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = openShiftUseCase(cashierId, cashierName, openingCash)
            result.fold(
                onSuccess = { shift ->
                    closeOpenShiftDialog()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Shift kasir '${shift.cashierName}' berhasil dibuka dengan modal awal Rp ${shift.openingCash.toInt()}"
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = err.message ?: "Gagal membuka shift kasir"
                    )
                }
            )
        }
    }

    // ========================================================================
    // CASH MOVEMENTS (CASH IN / CASH OUT / EXPENSE / REFUND)
    // ========================================================================

    fun openCashMovementDialog(type: CashMovementType = CashMovementType.EXPENSE) {
        _uiState.value = _uiState.value.copy(
            showCashMovementDialog = true,
            cashMovementType = type
        )
    }

    fun closeCashMovementDialog() {
        _uiState.value = _uiState.value.copy(showCashMovementDialog = false)
    }

    fun recordCashMovement(
        type: CashMovementType,
        amount: Double,
        reason: String,
        cashierId: String,
        cashierName: String
    ) {
        val shift = activeShift.value
        if (shift == null) {
            _uiState.value = _uiState.value.copy(error = "Tidak ada shift aktif untuk mencatat mutasi kas")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = recordCashMovementUseCase(
                shiftId = shift.id,
                type = type,
                amount = amount,
                reason = reason,
                cashierId = cashierId,
                cashierName = cashierName
            )
            result.fold(
                onSuccess = { movement ->
                    closeCashMovementDialog()
                    _movements.value = getCurrentShiftUseCase.getMovementsForShift(shift.id)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Mutasi kas (${movement.type.name}) sebesar Rp ${movement.amount.toInt()} berhasil dicatat"
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = err.message ?: "Gagal mencatat mutasi kas"
                    )
                }
            )
        }
    }

    // ========================================================================
    // CLOSE SHIFT & RECONCILIATION
    // ========================================================================

    fun openCloseShiftDialog() {
        val shift = activeShift.value
        if (shift != null) {
            viewModelScope.launch {
                // Refresh data penjualan riil terbaru sebelum buka dialog tutup shift
                getCurrentShiftUseCase.refreshShiftRealtimeSales(shift)
                _movements.value = getCurrentShiftUseCase.getMovementsForShift(shift.id)
                _uiState.value = _uiState.value.copy(showCloseShiftDialog = true)
            }
        }
    }

    fun closeCloseShiftDialog() {
        _uiState.value = _uiState.value.copy(showCloseShiftDialog = false)
    }

    fun closeShift(actualCash: Double, closingNotes: String?) {
        val shift = activeShift.value ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = closeShiftUseCase(
                shiftId = shift.id,
                actualCash = actualCash,
                closingNotes = closingNotes
            )
            result.fold(
                onSuccess = { closeResult ->
                    closeCloseShiftDialog()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showReportPreviewDialog = true,
                        reportPreviewTitle = "Z-REPORT (PENUTUPAN SHIFT)",
                        reportPreviewText = closeResult.zReportText,
                        successMessage = "Shift berhasil ditutup! Rekonsiliasi selesai."
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = err.message ?: "Gagal menutup shift"
                    )
                }
            )
        }
    }

    // ========================================================================
    // REPORTS (X-REPORT & Z-REPORT)
    // ========================================================================

    fun generateXReport() {
        val shift = activeShift.value ?: return
        viewModelScope.launch {
            val updated = getCurrentShiftUseCase.refreshShiftRealtimeSales(shift)
            val currentMovements = getCurrentShiftUseCase.getMovementsForShift(shift.id)
            val report = generateShiftReportUseCase.generateXReport(updated, currentMovements)

            _uiState.value = _uiState.value.copy(
                showReportPreviewDialog = true,
                reportPreviewTitle = "X-REPORT (MID-SHIFT AUDIT)",
                reportPreviewText = report
            )
        }
    }

    fun viewHistoricalZReport(shift: CashierShift) {
        viewModelScope.launch {
            val historyMovements = getCurrentShiftUseCase.getMovementsForShift(shift.id)
            val report = generateShiftReportUseCase.generateZReport(shift, historyMovements)

            _uiState.value = _uiState.value.copy(
                showReportPreviewDialog = true,
                reportPreviewTitle = "Z-REPORT HISTORIS (${shift.id.take(8)})",
                reportPreviewText = report
            )
        }
    }

    fun closeReportPreviewDialog() {
        _uiState.value = _uiState.value.copy(
            showReportPreviewDialog = false,
            reportPreviewText = null
        )
    }
}
