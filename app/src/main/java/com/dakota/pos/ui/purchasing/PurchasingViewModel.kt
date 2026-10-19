package com.dakota.pos.ui.purchasing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dakota.pos.domain.model.purchasing.GoodsReceipt
import com.dakota.pos.domain.model.purchasing.PurchaseOrder
import com.dakota.pos.domain.model.purchasing.PurchaseOrderItem
import com.dakota.pos.domain.model.purchasing.PurchaseOrderStatus
import com.dakota.pos.domain.model.purchasing.Supplier
import com.dakota.pos.domain.model.recipe.Ingredient
import com.dakota.pos.domain.usecase.purchasing.CreatePOUseCase
import com.dakota.pos.domain.usecase.purchasing.GetPurchasingDataUseCase
import com.dakota.pos.domain.usecase.purchasing.IncomingItemInput
import com.dakota.pos.domain.usecase.purchasing.ReceiveGoodsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class PurchasingTab {
    PO_LIST,
    CREATE_PO,
    GOODS_RECEIPTS,
    SUPPLIERS
}

data class PurchasingUiState(
    val isLoading: Boolean = false,
    val activeTab: PurchasingTab = PurchasingTab.PO_LIST,
    val selectedStatusFilter: PurchaseOrderStatus? = null,
    val error: String? = null,
    val successMessage: String? = null,

    // Dialog & Form states
    val showSupplierDialog: Boolean = false,
    val editingSupplier: Supplier? = null,

    val showReceiveDialog: Boolean = false,
    val selectedPoForReceiving: PurchaseOrder? = null,

    val showPoDetailDialog: Boolean = false,
    val selectedPoForDetail: PurchaseOrder? = null
)

class PurchasingViewModel(
    private val getPurchasingDataUseCase: GetPurchasingDataUseCase,
    private val createPOUseCase: CreatePOUseCase,
    private val receiveGoodsUseCase: ReceiveGoodsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchasingUiState())
    val uiState: StateFlow<PurchasingUiState> = _uiState.asStateFlow()

    val suppliers: StateFlow<List<Supplier>> = getPurchasingDataUseCase.getSuppliersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchaseOrders: StateFlow<List<PurchaseOrder>> = getPurchasingDataUseCase.getPurchaseOrdersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goodsReceipts: StateFlow<List<GoodsReceipt>> = getPurchasingDataUseCase.getGoodsReceiptsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ingredients: StateFlow<List<Ingredient>> = getPurchasingDataUseCase.getIngredientsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            getPurchasingDataUseCase.seedInitialSuppliers()
        }
    }

    fun setActiveTab(tab: PurchasingTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab, error = null, successMessage = null)
    }

    fun setStatusFilter(status: PurchaseOrderStatus?) {
        _uiState.value = _uiState.value.copy(selectedStatusFilter = status)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    // ========================================================================
    // SUPPLIERS
    // ========================================================================

    fun openSupplierDialog(supplier: Supplier? = null) {
        _uiState.value = _uiState.value.copy(
            showSupplierDialog = true,
            editingSupplier = supplier
        )
    }

    fun closeSupplierDialog() {
        _uiState.value = _uiState.value.copy(
            showSupplierDialog = false,
            editingSupplier = null
        )
    }

    fun saveSupplier(
        name: String,
        contactPerson: String?,
        phone: String,
        email: String?,
        address: String?,
        paymentTerms: String
    ) {
        if (name.isBlank() || phone.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Nama dan No. Telepon supplier wajib diisi")
            return
        }

        viewModelScope.launch {
            val existing = _uiState.value.editingSupplier
            val supplier = Supplier(
                id = existing?.id ?: UUID.randomUUID().toString(),
                name = name.trim(),
                contactPerson = contactPerson?.takeIf { it.isNotBlank() }?.trim(),
                phone = phone.trim(),
                email = email?.takeIf { it.isNotBlank() }?.trim(),
                address = address?.takeIf { it.isNotBlank() }?.trim(),
                paymentTerms = paymentTerms,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            getPurchasingDataUseCase.saveSupplier(supplier)
            closeSupplierDialog()
            _uiState.value = _uiState.value.copy(successMessage = "Supplier '${supplier.name}' berhasil disimpan")
        }
    }

    fun deleteSupplier(supplierId: String) {
        viewModelScope.launch {
            getPurchasingDataUseCase.deleteSupplier(supplierId)
            _uiState.value = _uiState.value.copy(successMessage = "Supplier berhasil dihapus")
        }
    }

    // ========================================================================
    // PURCHASE ORDER ACTIONS
    // ========================================================================

    fun createPurchaseOrder(
        supplierId: String,
        items: List<PurchaseOrderItem>,
        notes: String?,
        createdBy: String,
        submitImmediately: Boolean
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = createPOUseCase(
                supplierId = supplierId,
                items = items,
                notes = notes,
                createdBy = createdBy,
                submitImmediately = submitImmediately
            )
            result.fold(
                onSuccess = { po ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activeTab = PurchasingTab.PO_LIST,
                        successMessage = "PO ${po.poNumber} berhasil dibuat dengan status ${po.status}"
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = err.message ?: "Gagal membuat Purchase Order"
                    )
                }
            )
        }
    }

    fun submitPurchaseOrder(poId: String) {
        viewModelScope.launch {
            getPurchasingDataUseCase.updateOrderStatus(poId, PurchaseOrderStatus.SUBMITTED)
            _uiState.value = _uiState.value.copy(successMessage = "Status PO berhasil diubah menjadi SUBMITTED")
        }
    }

    fun cancelPurchaseOrder(poId: String) {
        viewModelScope.launch {
            getPurchasingDataUseCase.updateOrderStatus(poId, PurchaseOrderStatus.CANCELLED)
            _uiState.value = _uiState.value.copy(successMessage = "PO berhasil dibatalkan (CANCELLED)")
        }
    }

    fun openPoDetail(po: PurchaseOrder) {
        _uiState.value = _uiState.value.copy(
            showPoDetailDialog = true,
            selectedPoForDetail = po
        )
    }

    fun closePoDetail() {
        _uiState.value = _uiState.value.copy(
            showPoDetailDialog = false,
            selectedPoForDetail = null
        )
    }

    // ========================================================================
    // GOODS RECEIVING
    // ========================================================================

    fun openReceiveDialog(po: PurchaseOrder) {
        _uiState.value = _uiState.value.copy(
            showReceiveDialog = true,
            selectedPoForReceiving = po
        )
    }

    fun closeReceiveDialog() {
        _uiState.value = _uiState.value.copy(
            showReceiveDialog = false,
            selectedPoForReceiving = null
        )
    }

    fun processGoodsReceipt(
        poId: String,
        receivedBy: String,
        items: List<IncomingItemInput>,
        notes: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = receiveGoodsUseCase(
                purchaseOrderId = poId,
                receivedBy = receivedBy,
                receivedItems = items,
                receiptNotes = notes
            )
            result.fold(
                onSuccess = { gr ->
                    closeReceiveDialog()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Penerimaan barang ${gr.receiptNumber} untuk PO ${gr.poNumber} berhasil diproses. Stok dan HPP telah diperbarui!"
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = err.message ?: "Gagal memproses penerimaan barang"
                    )
                }
            )
        }
    }
}
