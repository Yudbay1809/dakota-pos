package com.dakota.pos.presentation.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dakota.pos.core.hardware.printer.PrintResult
import com.dakota.pos.domain.model.Cart
import com.dakota.pos.domain.model.Category
import com.dakota.pos.domain.model.Modifier
import com.dakota.pos.domain.model.Order
import com.dakota.pos.domain.model.Product
import com.dakota.pos.domain.model.ProductVariant
import com.dakota.pos.domain.payment.PaymentMethodType
import com.dakota.pos.domain.payment.PaymentResult
import com.dakota.pos.domain.repository.ProductRepository
import com.dakota.pos.domain.usecase.payment.PrintReceiptUseCase
import com.dakota.pos.domain.usecase.payment.ProcessPaymentUseCase
import com.dakota.pos.domain.usecase.pos.CalculateCartUseCase
import com.dakota.pos.domain.usecase.pos.CheckoutOrderUseCase
import com.dakota.pos.domain.usecase.pos.GetCatalogUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PosUiState(
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
    val modifiers: List<Modifier> = emptyList(),
    val cart: Cart = Cart(),
    val selectedCategoryId: String? = null,
    val isLoading: Boolean = true,

    // Product detail / add-to-cart dialog
    val selectedProduct: Product? = null,
    val selectedVariant: ProductVariant? = null,
    val selectedModifiers: List<Modifier> = emptyList(),
    val itemQuantity: Int = 1,
    val itemNotes: String = "",
    val showProductDialog: Boolean = false,

    // Payment Dialog (Phase 6)
    val showPaymentDialog: Boolean = false,
    val selectedPaymentMethod: PaymentMethodType = PaymentMethodType.CASH,
    val cashReceivedInput: String = "",
    val cashReceivedAmount: Double = 0.0,
    val changeAmount: Double = 0.0,
    val referenceNumberInput: String = "",
    val isProcessingPayment: Boolean = false,
    val paymentErrorMessage: String? = null,

    // Checkout & Receipt (Phase 6)
    val lastOrder: Order? = null,
    val lastPaymentResult: PaymentResult.Success? = null,
    val receiptText: String = "",
    val showReceiptPreview: Boolean = false,
    val printStatusMessage: String? = null,
    val checkoutMessage: String? = null,
    val showCheckoutSuccess: Boolean = false
)

class PosViewModel(
    private val getCatalogUseCase: GetCatalogUseCase,
    private val calculateCartUseCase: CalculateCartUseCase,
    private val checkoutOrderUseCase: CheckoutOrderUseCase,
    private val processPaymentUseCase: ProcessPaymentUseCase,
    private val printReceiptUseCase: PrintReceiptUseCase,
    private val productRepository: ProductRepository,
    val cashierId: String,
    val cashierName: String,
    private val dispatchOrderToKitchenUseCase: com.dakota.pos.domain.usecase.kds.DispatchOrderToKitchenUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            // Seed catalog data on first launch
            productRepository.seedInitialCatalogIfEmpty()

            getCatalogUseCase().collect { catalogData ->
                _uiState.update { state ->
                    state.copy(
                        categories = catalogData.categories,
                        products = catalogData.products,
                        modifiers = catalogData.modifiers,
                        isLoading = false,
                        selectedCategoryId = state.selectedCategoryId ?: catalogData.categories.firstOrNull()?.id
                    )
                }
            }
        }
    }

    fun selectCategory(categoryId: String) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    val filteredProducts: List<Product>
        get() {
            val state = _uiState.value
            return if (state.selectedCategoryId == null) {
                state.products
            } else {
                state.products.filter { it.categoryId == state.selectedCategoryId }
            }
        }

    // --- Product Selection Dialog ---

    fun onProductTapped(product: Product) {
        viewModelScope.launch {
            val fullProduct = productRepository.getProductById(product.id) ?: product
            val defaultVariant = fullProduct.variants.firstOrNull { it.isDefault }
                ?: fullProduct.variants.firstOrNull()

            _uiState.update {
                it.copy(
                    selectedProduct = fullProduct,
                    selectedVariant = defaultVariant,
                    selectedModifiers = emptyList(),
                    itemQuantity = 1,
                    itemNotes = "",
                    showProductDialog = true
                )
            }
        }
    }

    fun selectVariant(variant: ProductVariant) {
        _uiState.update { it.copy(selectedVariant = variant) }
    }

    fun toggleModifier(modifier: Modifier) {
        _uiState.update { state ->
            val current = state.selectedModifiers.toMutableList()
            if (current.any { it.id == modifier.id }) {
                current.removeAll { it.id == modifier.id }
            } else {
                current.add(modifier)
            }
            state.copy(selectedModifiers = current)
        }
    }

    fun setItemQuantity(qty: Int) {
        if (qty in 1..99) {
            _uiState.update { it.copy(itemQuantity = qty) }
        }
    }

    fun setItemNotes(notes: String) {
        _uiState.update { it.copy(itemNotes = notes) }
    }

    fun dismissProductDialog() {
        _uiState.update {
            it.copy(
                showProductDialog = false,
                selectedProduct = null,
                selectedVariant = null,
                selectedModifiers = emptyList(),
                itemQuantity = 1,
                itemNotes = ""
            )
        }
    }

    fun confirmAddToCart() {
        val state = _uiState.value
        val product = state.selectedProduct ?: return

        val newCart = calculateCartUseCase.addItem(
            cart = state.cart,
            product = product,
            variant = state.selectedVariant,
            modifiers = state.selectedModifiers,
            quantity = state.itemQuantity,
            notes = state.itemNotes.takeIf { it.isNotBlank() }
        )

        _uiState.update {
            it.copy(
                cart = newCart,
                showProductDialog = false,
                selectedProduct = null,
                selectedVariant = null,
                selectedModifiers = emptyList(),
                itemQuantity = 1,
                itemNotes = ""
            )
        }
    }

    // --- Cart Management ---

    fun increaseCartItemQty(itemId: String) {
        _uiState.update {
            it.copy(cart = calculateCartUseCase.updateQuantity(it.cart, itemId, 1))
        }
    }

    fun decreaseCartItemQty(itemId: String) {
        _uiState.update {
            it.copy(cart = calculateCartUseCase.updateQuantity(it.cart, itemId, -1))
        }
    }

    fun removeCartItem(itemId: String) {
        _uiState.update {
            it.copy(cart = calculateCartUseCase.removeItem(it.cart, itemId))
        }
    }

    fun clearCart() {
        _uiState.update {
            it.copy(cart = calculateCartUseCase.clearCart())
        }
    }

    // --- Payment Flow (Phase 6) ---

    fun openPaymentDialog() {
        val cart = _uiState.value.cart
        if (cart.items.isEmpty()) {
            _uiState.update { it.copy(checkoutMessage = "Keranjang kosong. Tambahkan item terlebih dahulu.") }
            return
        }

        _uiState.update {
            it.copy(
                showPaymentDialog = true,
                selectedPaymentMethod = PaymentMethodType.CASH,
                cashReceivedInput = "",
                cashReceivedAmount = 0.0,
                changeAmount = 0.0,
                referenceNumberInput = "",
                paymentErrorMessage = null
            )
        }
    }

    fun dismissPaymentDialog() {
        _uiState.update {
            it.copy(
                showPaymentDialog = false,
                paymentErrorMessage = null,
                isProcessingPayment = false
            )
        }
    }

    fun selectPaymentMethod(method: PaymentMethodType) {
        _uiState.update {
            it.copy(
                selectedPaymentMethod = method,
                paymentErrorMessage = null
            )
        }
    }

    fun onCashInputChange(input: String) {
        val parsed = input.toDoubleOrNull() ?: 0.0
        val total = _uiState.value.cart.total
        val change = if (parsed >= total) parsed - total else 0.0

        _uiState.update {
            it.copy(
                cashReceivedInput = input,
                cashReceivedAmount = parsed,
                changeAmount = change,
                paymentErrorMessage = null
            )
        }
    }

    fun onQuickCashSelected(amount: Double) {
        val total = _uiState.value.cart.total
        val change = if (amount >= total) amount - total else 0.0

        _uiState.update {
            it.copy(
                cashReceivedInput = amount.toLong().toString(),
                cashReceivedAmount = amount,
                changeAmount = change,
                paymentErrorMessage = null
            )
        }
    }

    fun onReferenceNumberChange(reference: String) {
        _uiState.update {
            it.copy(referenceNumberInput = reference)
        }
    }

    fun confirmPayment() {
        viewModelScope.launch {
            val state = _uiState.value
            val cart = state.cart

            if (cart.items.isEmpty()) {
                _uiState.update { it.copy(paymentErrorMessage = "Keranjang kosong.") }
                return@launch
            }

            _uiState.update { it.copy(isProcessingPayment = true, paymentErrorMessage = null) }

            // 1. Buat pesanan terlebih dahulu
            val checkoutResult = checkoutOrderUseCase(
                cart = cart,
                cashierId = cashierId,
                cashierName = cashierName
            )

            val order = when (checkoutResult) {
                is CheckoutOrderUseCase.Result.Success -> checkoutResult.order
                is CheckoutOrderUseCase.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessingPayment = false,
                            paymentErrorMessage = checkoutResult.message
                        )
                    }
                    return@launch
                }
            }

            // 2. Proses pembayaran melalui PaymentProcessor
            val paymentResult = processPaymentUseCase(
                orderId = order.id,
                method = state.selectedPaymentMethod,
                cashReceived = if (state.selectedPaymentMethod == PaymentMethodType.CASH) state.cashReceivedAmount else null,
                referenceNumber = state.referenceNumberInput.takeIf { it.isNotBlank() }
            )

            when (paymentResult) {
                is ProcessPaymentUseCase.Result.Success -> {
                    // 3. Format dan Cetak Struk (Mock Printer)
                    val printResult = printReceiptUseCase(
                        order = paymentResult.order,
                        cashReceived = if (state.selectedPaymentMethod == PaymentMethodType.CASH) state.cashReceivedAmount else null,
                        change = if (state.selectedPaymentMethod == PaymentMethodType.CASH) state.changeAmount else 0.0,
                        referenceNumber = paymentResult.paymentResult.transactionReference
                    )

                    // Jika tunai, buka laci kasir otomatis
                    if (state.selectedPaymentMethod == PaymentMethodType.CASH) {
                        printReceiptUseCase.openCashDrawer()
                    }

                    // 4. Dispatch pesanan ke KDS & station printer (Phase 11)
                    runCatching {
                        dispatchOrderToKitchenUseCase?.invoke(paymentResult.order)
                    }

                    _uiState.update {
                        it.copy(
                            cart = Cart(),
                            lastOrder = paymentResult.order,
                            lastPaymentResult = paymentResult.paymentResult,
                            receiptText = printResult.formattedText,
                            showPaymentDialog = false,
                            isProcessingPayment = false,
                            showCheckoutSuccess = true,
                            checkoutMessage = "Pembayaran ${paymentResult.order.orderNumber} berhasil!"
                        )
                    }
                }
                is ProcessPaymentUseCase.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessingPayment = false,
                            paymentErrorMessage = paymentResult.message
                        )
                    }
                }
            }
        }
    }

    // --- Receipt & Hardware Actions ---

    fun reprintReceipt() {
        viewModelScope.launch {
            val state = _uiState.value
            val order = state.lastOrder ?: return@launch

            val result = printReceiptUseCase(
                order = order,
                cashReceived = if (state.selectedPaymentMethod == PaymentMethodType.CASH) state.cashReceivedAmount else null,
                change = if (state.selectedPaymentMethod == PaymentMethodType.CASH) state.changeAmount else 0.0,
                referenceNumber = state.lastPaymentResult?.transactionReference
            )

            val msg = when (result.printResult) {
                is PrintResult.Success -> "Struk berhasil dicetak!"
                is PrintResult.Failure -> "Gagal mencetak struk: ${result.printResult.error}"
            }
            _uiState.update { it.copy(printStatusMessage = msg) }
        }
    }

    fun openCashDrawer() {
        viewModelScope.launch {
            val opened = printReceiptUseCase.openCashDrawer()
            _uiState.update {
                it.copy(printStatusMessage = if (opened) "Laci kasir dibuka!" else "Gagal membuka laci kasir")
            }
        }
    }

    fun showReceiptPreview() {
        _uiState.update { it.copy(showReceiptPreview = true) }
    }

    fun dismissReceiptPreview() {
        _uiState.update { it.copy(showReceiptPreview = false) }
    }

    fun dismissCheckoutSuccess() {
        _uiState.update {
            it.copy(
                showCheckoutSuccess = false,
                checkoutMessage = null,
                lastOrder = null,
                lastPaymentResult = null,
                printStatusMessage = null
            )
        }
    }

    fun dismissMessage() {
        _uiState.update {
            it.copy(
                checkoutMessage = null,
                printStatusMessage = null
            )
        }
    }
}
