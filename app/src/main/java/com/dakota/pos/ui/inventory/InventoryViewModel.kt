package com.dakota.pos.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dakota.pos.domain.model.inventory.StockMovement
import com.dakota.pos.domain.model.recipe.Ingredient
import com.dakota.pos.domain.usecase.inventory.GetInventoryDataUseCase
import com.dakota.pos.domain.usecase.inventory.ManualStockMovementUseCase
import com.dakota.pos.domain.usecase.inventory.PerformStockOpnameUseCase
import com.dakota.pos.domain.usecase.inventory.RecordWastageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InventoryUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val showStockInDialog: Boolean = false,
    val showStockOutDialog: Boolean = false,
    val showWastageDialog: Boolean = false,
    val showOpnameDialog: Boolean = false,
    val selectedIngredient: Ingredient? = null,
    val activeTab: InventoryTab = InventoryTab.MONITORING
)

enum class InventoryTab {
    MONITORING, MOVEMENTS, OPNAME
}

class InventoryViewModel(
    private val getInventoryDataUseCase: GetInventoryDataUseCase,
    private val manualStockMovementUseCase: ManualStockMovementUseCase,
    private val recordWastageUseCase: RecordWastageUseCase,
    private val performStockOpnameUseCase: PerformStockOpnameUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    val ingredients: StateFlow<List<Ingredient>> = getInventoryDataUseCase.getIngredientsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentMovements: StateFlow<List<StockMovement>> = getInventoryDataUseCase.getRecentMovements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setActiveTab(tab: InventoryTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    fun showStockInDialog(ingredient: Ingredient) {
        _uiState.value = _uiState.value.copy(
            showStockInDialog = true,
            selectedIngredient = ingredient
        )
    }

    fun showStockOutDialog(ingredient: Ingredient) {
        _uiState.value = _uiState.value.copy(
            showStockOutDialog = true,
            selectedIngredient = ingredient
        )
    }

    fun showWastageDialog(ingredient: Ingredient) {
        _uiState.value = _uiState.value.copy(
            showWastageDialog = true,
            selectedIngredient = ingredient
        )
    }

    fun showOpnameDialog() {
        _uiState.value = _uiState.value.copy(showOpnameDialog = true)
    }

    fun dismissDialogs() {
        _uiState.value = _uiState.value.copy(
            showStockInDialog = false,
            showStockOutDialog = false,
            showWastageDialog = false,
            showOpnameDialog = false,
            selectedIngredient = null,
            error = null,
            successMessage = null
        )
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    fun performStockIn(
        ingredientId: String,
        quantity: Double,
        notes: String?,
        userId: String,
        userName: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                manualStockMovementUseCase.stockIn(ingredientId, quantity, notes, userId, userName)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Stok masuk berhasil dicatat",
                    showStockInDialog = false,
                    selectedIngredient = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Gagal mencatat stok masuk"
                )
            }
        }
    }

    fun performStockOut(
        ingredientId: String,
        quantity: Double,
        notes: String?,
        userId: String,
        userName: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                manualStockMovementUseCase.stockOut(ingredientId, quantity, notes, userId, userName)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Stok keluar berhasil dicatat",
                    showStockOutDialog = false,
                    selectedIngredient = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Gagal mencatat stok keluar"
                )
            }
        }
    }

    fun performWastage(
        ingredientId: String,
        quantity: Double,
        reason: String,
        userId: String,
        userName: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                recordWastageUseCase(ingredientId, quantity, reason, userId, userName)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Wastage berhasil dicatat",
                    showWastageDialog = false,
                    selectedIngredient = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Gagal mencatat wastage"
                )
            }
        }
    }

    fun performStockOpname(
        physicalCounts: Map<String, Double>,
        userId: String,
        userName: String,
        notes: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                performStockOpnameUseCase(physicalCounts, userId, userName, notes)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Stock Opname berhasil disimpan",
                    showOpnameDialog = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Gagal menyimpan stock opname"
                )
            }
        }
    }

    class Factory(
        private val getInventoryDataUseCase: GetInventoryDataUseCase,
        private val manualStockMovementUseCase: ManualStockMovementUseCase,
        private val recordWastageUseCase: RecordWastageUseCase,
        private val performStockOpnameUseCase: PerformStockOpnameUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InventoryViewModel(
                getInventoryDataUseCase,
                manualStockMovementUseCase,
                recordWastageUseCase,
                performStockOpnameUseCase
            ) as T
        }
    }
}
