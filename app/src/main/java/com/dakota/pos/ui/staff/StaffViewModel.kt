package com.dakota.pos.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dakota.pos.core.hardware.printer.PrinterInterface
import com.dakota.pos.domain.auth.UserRole
import com.dakota.pos.domain.model.staff.*
import com.dakota.pos.domain.repository.StaffAttendanceRepository
import com.dakota.pos.domain.usecase.staff.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StaffUiState(
    val attendances: List<StaffAttendance> = emptyList(),
    val salaryRates: List<StaffSalaryRate> = emptyList(),
    val selectedUserId: String = "USR-001",
    val currentPayroll: StaffPayrollSummary? = null,
    val periodMonth: String = "Mei 2026",
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val receiptPreviewText: String? = null,
    val showReceiptDialog: Boolean = false,
    val showScanDialog: Boolean = false,
    val showAdjustmentDialog: Boolean = false,
    val notificationMessage: String? = null
)

class StaffViewModel(
    private val staffAttendanceRepository: StaffAttendanceRepository,
    private val recordStaffAttendanceUseCase: RecordStaffAttendanceUseCase,
    private val syncBiometricDeviceUseCase: SyncBiometricDeviceUseCase,
    private val calculateStaffPayrollUseCase: CalculateStaffPayrollUseCase,
    private val saveStaffPayrollUseCase: SaveStaffPayrollUseCase,
    private val generatePaySlipReceiptUseCase: GeneratePaySlipReceiptUseCase,
    private val printer: PrinterInterface? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(StaffUiState())
    val uiState: StateFlow<StaffUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeAttendances()
    }

    private fun loadData() {
        viewModelScope.launch {
            val rates = staffAttendanceRepository.getSalaryRates()
            val effectiveRates = if (rates.isNotEmpty()) {
                rates
            } else {
                listOf(
                    StaffSalaryRate("USR-001", "Budi Barista", UserRole.BARISTA, 4_000_000.0),
                    StaffSalaryRate("USR-002", "Siti Kasir", UserRole.CASHIER, 4_200_000.0),
                    StaffSalaryRate("USR-003", "Andi Supervisor", UserRole.MANAGER, 6_500_000.0),
                    StaffSalaryRate("USR-004", "Doni Owner", UserRole.OWNER, 10_000_000.0)
                )
            }

            val defaultUserId = effectiveRates.firstOrNull()?.userId ?: "USR-001"
            _uiState.update {
                it.copy(
                    salaryRates = effectiveRates,
                    selectedUserId = defaultUserId
                )
            }

            recalculatePayroll(defaultUserId)
        }
    }

    private fun observeAttendances() {
        viewModelScope.launch {
            staffAttendanceRepository.getAllAttendancesFlow().collect { logs ->
                _uiState.update { it.copy(attendances = logs) }
                recalculatePayroll(_uiState.value.selectedUserId)
            }
        }
    }

    fun selectStaff(userId: String) {
        _uiState.update { it.copy(selectedUserId = userId) }
        recalculatePayroll(userId)
    }

    fun syncFromBiometricDevice() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncMessage = "Menghubungi buffer mesin absensi...") }
            val result = syncBiometricDeviceUseCase(simulateSimulatedRecords = true)
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    syncMessage = result.message,
                    notificationMessage = result.message
                )
            }
            recalculatePayroll(_uiState.value.selectedUserId)
        }
    }

    fun recordQuickScan(
        userId: String,
        type: AttendanceType,
        source: AttendanceSource = AttendanceSource.POS_MANUAL_SIMULATION,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val staff = _uiState.value.salaryRates.find { it.userId == userId }
                ?: StaffSalaryRate(userId, "Staff Kafe", UserRole.BARISTA, 4_000_000.0)

            val rec = recordStaffAttendanceUseCase(
                userId = userId,
                staffName = staff.staffName,
                role = staff.role,
                biometricId = "BIO-" + userId.takeLast(3),
                type = type,
                source = source,
                notes = notes
            )

            val actionDesc = if (type == AttendanceType.IN) "Masuk (Clock-IN)" else "Pulang (Clock-OUT)"
            _uiState.update {
                it.copy(
                    notificationMessage = "Presensi ${staff.staffName} $actionDesc berhasil dicatat! Status: ${rec.status.name}",
                    showScanDialog = false,
                    showAdjustmentDialog = false
                )
            }

            recalculatePayroll(userId)
        }
    }

    fun recalculatePayroll(userId: String) {
        viewModelScope.launch {
            val payroll = calculateStaffPayrollUseCase(
                userId = userId,
                periodMonth = _uiState.value.periodMonth
            )
            _uiState.update { it.copy(currentPayroll = payroll) }
        }
    }

    fun generateAndShowSlipPreview() {
        val payroll = _uiState.value.currentPayroll ?: return
        val receiptText = generatePaySlipReceiptUseCase(payroll)
        _uiState.update {
            it.copy(
                receiptPreviewText = receiptText,
                showReceiptDialog = true
            )
        }
    }

    fun printSlipToThermalPrinter() {
        val text = _uiState.value.receiptPreviewText ?: return
        viewModelScope.launch {
            printer?.printRawBytes(text.toByteArray(Charsets.UTF_8))
            printer?.cutPaper()
            _uiState.update {
                it.copy(
                    notificationMessage = "Slip Gaji berhasil dikirim ke Thermal Receipt Printer POS!"
                )
            }
        }
    }

    fun savePayrollToDatabaseAndFinance() {
        val payroll = _uiState.value.currentPayroll ?: return
        viewModelScope.launch {
            saveStaffPayrollUseCase(payroll, postToOperatingExpenses = true)
            _uiState.update {
                it.copy(
                    notificationMessage = "Slip Gaji ${payroll.staffName} disimpan & tercatat ke Laporan Laba/Rugi (Beban Operasional)!"
                )
            }
        }
    }

    fun dismissReceiptDialog() {
        _uiState.update { it.copy(showReceiptDialog = false) }
    }

    fun openScanDialog() {
        _uiState.update { it.copy(showScanDialog = true) }
    }

    fun dismissScanDialog() {
        _uiState.update { it.copy(showScanDialog = false) }
    }

    fun openAdjustmentDialog() {
        _uiState.update { it.copy(showAdjustmentDialog = true) }
    }

    fun dismissAdjustmentDialog() {
        _uiState.update { it.copy(showAdjustmentDialog = false) }
    }

    fun clearNotification() {
        _uiState.update { it.copy(notificationMessage = null) }
    }
}
