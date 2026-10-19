package com.dakota.pos.ui.kds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dakota.pos.domain.model.kds.KitchenOrder
import com.dakota.pos.domain.model.kds.KitchenOrderStatus
import com.dakota.pos.domain.model.kds.StationConfig
import com.dakota.pos.domain.model.kds.WorkStation
import com.dakota.pos.domain.usecase.kds.GetKdsOrdersUseCase
import com.dakota.pos.domain.usecase.kds.ManageStationConfigUseCase
import com.dakota.pos.domain.usecase.kds.PrintStationTicketUseCase
import com.dakota.pos.domain.usecase.kds.UpdateKitchenOrderStatusUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KdsUiState(
    val selectedStation: WorkStation? = null,
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val showConfigDialog: Boolean = false,
    val selectedConfig: StationConfig? = null,
    val isPrinting: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class KdsViewModel(
    private val getKdsOrdersUseCase: GetKdsOrdersUseCase,
    private val updateKitchenOrderStatusUseCase: UpdateKitchenOrderStatusUseCase,
    private val printStationTicketUseCase: PrintStationTicketUseCase,
    private val manageStationConfigUseCase: ManageStationConfigUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(KdsUiState())
    val uiState: StateFlow<KdsUiState> = _uiState.asStateFlow()

    private val _selectedStation = MutableStateFlow<WorkStation?>(null)

    val activeOrders: StateFlow<List<KitchenOrder>> = _selectedStation
        .flatMapLatest { station -> getKdsOrdersUseCase(station) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stationConfigs: StateFlow<List<StationConfig>> = manageStationConfigUseCase.getConfigsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            manageStationConfigUseCase.initDefaults()
        }
    }

    fun selectStation(station: WorkStation?) {
        _selectedStation.value = station
        _uiState.update { it.copy(selectedStation = station) }
    }

    fun advanceOrderStatus(orderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = updateKitchenOrderStatusUseCase.advanceStatus(orderId)
            result.fold(
                onSuccess = { nextStatus ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = "Status pesanan berhasil diperbarui ke ${nextStatus.displayName}"
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = err.message ?: "Gagal memperbarui status pesanan"
                        )
                    }
                }
            )
        }
    }

    fun updateOrderStatus(orderId: String, status: KitchenOrderStatus) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = updateKitchenOrderStatusUseCase(orderId, status)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = "Status pesanan diubah menjadi ${status.displayName}"
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = err.message ?: "Gagal mengubah status pesanan"
                        )
                    }
                }
            )
        }
    }

    fun reprintTicket(order: KitchenOrder) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPrinting = true, error = null) }
            val result = printStationTicketUseCase(order)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isPrinting = false,
                            message = "Tiket pes #${order.orderNumber} berhasil dicetak ulang!"
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isPrinting = false,
                            error = "Gagal cetak tiket: ${err.message}"
                        )
                    }
                }
            )
        }
    }

    fun openConfigDialog(config: StationConfig? = null) {
        _uiState.update { it.copy(showConfigDialog = true, selectedConfig = config) }
    }

    fun closeConfigDialog() {
        _uiState.update { it.copy(showConfigDialog = false, selectedConfig = null) }
    }

    fun saveConfig(config: StationConfig) {
        viewModelScope.launch {
            manageStationConfigUseCase.saveConfig(config)
            _uiState.update {
                it.copy(
                    showConfigDialog = false,
                    selectedConfig = null,
                    message = "Konfigurasi stasiun ${config.name} berhasil disimpan"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
