package com.dakota.pos.presentation.pos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.dakota.pos.presentation.payment.PaymentDialog
import com.dakota.pos.presentation.payment.ReceiptPreviewDialog
import com.dakota.pos.presentation.recipe.RecipeManagementDialog
import com.dakota.pos.presentation.recipe.RecipeViewModel
import com.dakota.pos.domain.auth.Permission
import com.dakota.pos.domain.auth.UserRole
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dakota.pos.domain.auth.UserSession
import com.dakota.pos.domain.model.CartItem
import com.dakota.pos.domain.model.Category
import com.dakota.pos.domain.model.Product
import com.dakota.pos.presentation.auth.RoleBadge
import com.dakota.pos.presentation.theme.CoffeeCaramel
import com.dakota.pos.presentation.theme.CoffeeCream
import com.dakota.pos.presentation.theme.CoffeeEspresso
import com.dakota.pos.presentation.theme.CoffeeEspressoDark
import com.dakota.pos.presentation.theme.CoffeeLatte
import com.dakota.pos.presentation.theme.CoffeeWarmBrown
import com.dakota.pos.presentation.theme.PosBackgroundDark
import com.dakota.pos.presentation.theme.PosBorderDark
import com.dakota.pos.presentation.theme.PosSurfaceDark
import com.dakota.pos.presentation.theme.PosSurfaceElevatedDark
import com.dakota.pos.presentation.theme.PosTextMutedDark
import com.dakota.pos.presentation.theme.PosTextPrimaryDark
import com.dakota.pos.presentation.theme.PosTextSecondaryDark
import com.dakota.pos.presentation.theme.StatusError
import com.dakota.pos.presentation.theme.StatusSuccess
import java.text.NumberFormat
import java.util.Locale

private val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
    maximumFractionDigits = 0
}

private fun formatRupiah(amount: Double): String = rupiahFormat.format(amount)

// ============================================================================
//  MAIN POS SCREEN — Split Layout: Catalog (Left) + Cart (Right)
// ============================================================================

@Composable
fun PosScreen(
    viewModel: PosViewModel,
    session: UserSession,
    recipeViewModel: RecipeViewModel? = null,
    inventoryViewModel: com.dakota.pos.ui.inventory.InventoryViewModel? = null,
    purchasingViewModel: com.dakota.pos.ui.purchasing.PurchasingViewModel? = null,
    shiftViewModel: com.dakota.pos.ui.shift.ShiftViewModel? = null,
    kdsViewModel: com.dakota.pos.ui.kds.KdsViewModel? = null,
    financeViewModel: com.dakota.pos.ui.finance.FinanceViewModel? = null,
    staffViewModel: com.dakota.pos.ui.staff.StaffViewModel? = null,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRecipeManagement by remember { mutableStateOf(false) }
    var showInventory by remember { mutableStateOf(false) }
    var showPurchasing by remember { mutableStateOf(false) }
    var showShiftManagement by remember { mutableStateOf(false) }
    var showKds by remember { mutableStateOf(false) }
    var showFinance by remember { mutableStateOf(false) }
    var showStaffPayroll by remember { mutableStateOf(false) }
    val canViewHpp = session.role == UserRole.OWNER || session.permissions.contains(Permission.VIEW_HPP)

    Box(modifier = Modifier.fillMaxSize().background(PosBackgroundDark)) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Panel: Catalog (65% width)
            Column(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight()
                    .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp)
            ) {
                // Top Bar
                PosTopBar(
                    session = session,
                    canViewHpp = canViewHpp,
                    onOpenRecipeManagement = { showRecipeManagement = true },
                    onOpenInventory = { showInventory = true },
                    onOpenPurchasing = { showPurchasing = true },
                    onOpenShiftManagement = { showShiftManagement = true },
                    onOpenKds = { showKds = true },
                    onOpenFinance = { showFinance = true },
                    onOpenStaffPayroll = { showStaffPayroll = true },
                    onLogout = onLogout
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Category Tabs
                CategoryTabRow(
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    onSelectCategory = { viewModel.selectCategory(it) }
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Product Grid
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CoffeeCaramel)
                    }
                } else {
                    val displayed = viewModel.filteredProducts
                    ProductGrid(
                        products = displayed,
                        onProductTap = { viewModel.onProductTapped(it) }
                    )
                }
            }

            // Right Panel: Cart (35% width)
            CartPanel(
                cart = uiState.cart,
                onIncrease = { viewModel.increaseCartItemQty(it) },
                onDecrease = { viewModel.decreaseCartItemQty(it) },
                onRemove = { viewModel.removeCartItem(it) },
                onClearCart = { viewModel.clearCart() },
                onCheckout = { viewModel.openPaymentDialog() },
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
                    .padding(start = 8.dp, top = 16.dp, bottom = 16.dp, end = 16.dp)
            )
        }

        // Product Detail Dialog
        if (uiState.showProductDialog && uiState.selectedProduct != null) {
            ProductDetailDialog(
                product = uiState.selectedProduct!!,
                variants = uiState.selectedProduct!!.variants,
                modifiers = uiState.modifiers,
                selectedVariant = uiState.selectedVariant,
                selectedModifiers = uiState.selectedModifiers,
                quantity = uiState.itemQuantity,
                notes = uiState.itemNotes,
                onSelectVariant = { viewModel.selectVariant(it) },
                onToggleModifier = { viewModel.toggleModifier(it) },
                onSetQuantity = { viewModel.setItemQuantity(it) },
                onSetNotes = { viewModel.setItemNotes(it) },
                onConfirm = { viewModel.confirmAddToCart() },
                onDismiss = { viewModel.dismissProductDialog() }
            )
        }

        // Payment Dialog (Phase 6)
        if (uiState.showPaymentDialog) {
            PaymentDialog(
                totalAmount = uiState.cart.total,
                selectedMethod = uiState.selectedPaymentMethod,
                cashReceivedInput = uiState.cashReceivedInput,
                cashReceivedAmount = uiState.cashReceivedAmount,
                changeAmount = uiState.changeAmount,
                referenceNumber = uiState.referenceNumberInput,
                isProcessing = uiState.isProcessingPayment,
                errorMessage = uiState.paymentErrorMessage,
                onSelectMethod = { viewModel.selectPaymentMethod(it) },
                onCashInputChange = { viewModel.onCashInputChange(it) },
                onQuickCashSelected = { viewModel.onQuickCashSelected(it) },
                onReferenceNumberChange = { viewModel.onReferenceNumberChange(it) },
                onConfirmPayment = { viewModel.confirmPayment() },
                onDismiss = { viewModel.dismissPaymentDialog() }
            )
        }

        // Receipt Preview Dialog (Phase 6)
        if (uiState.showReceiptPreview && uiState.receiptText.isNotBlank()) {
            ReceiptPreviewDialog(
                receiptText = uiState.receiptText,
                onPrintClick = { viewModel.reprintReceipt() },
                onOpenDrawerClick = { viewModel.openCashDrawer() },
                onDismiss = { viewModel.dismissReceiptPreview() }
            )
        }

        // Checkout Success Overlay
        if (uiState.showCheckoutSuccess && uiState.lastOrder != null) {
            CheckoutSuccessOverlay(
                order = uiState.lastOrder!!,
                paymentResult = uiState.lastPaymentResult,
                printStatusMessage = uiState.printStatusMessage,
                onPrintReceipt = { viewModel.reprintReceipt() },
                onOpenDrawer = { viewModel.openCashDrawer() },
                onShowPreview = { viewModel.showReceiptPreview() },
                onDismiss = { viewModel.dismissCheckoutSuccess() }
            )
        }

        // Recipe Management Dialog (Phase 7)
        if (showRecipeManagement && recipeViewModel != null) {
            RecipeManagementDialog(
                viewModel = recipeViewModel,
                onDismiss = { showRecipeManagement = false }
            )
        }

        // Inventory Management Dialog (Phase 8)
        if (showInventory && inventoryViewModel != null) {
            Dialog(
                onDismissRequest = { showInventory = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1A1A2E)
                ) {
                    Box {
                        com.dakota.pos.ui.inventory.InventoryScreen(
                            viewModel = inventoryViewModel
                        )
                        IconButton(
                            onClick = { showInventory = false },
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        ) {
                            Icon(Icons.Default.Close, "Tutup", tint = Color.White)
                        }
                    }
                }
            }
        }

        // Purchasing Management Dialog (Phase 9)
        if (showPurchasing && purchasingViewModel != null) {
            Dialog(
                onDismissRequest = { showPurchasing = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1A1A2E)
                ) {
                    Box {
                        com.dakota.pos.ui.purchasing.PurchasingScreen(
                            viewModel = purchasingViewModel,
                            currentUserName = session.username
                        )
                        IconButton(
                            onClick = { showPurchasing = false },
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        ) {
                            Icon(Icons.Default.Close, "Tutup", tint = Color.White)
                        }
                    }
                }
            }
        }

        // Shift & Cash Management Dialog (Phase 10)
        if (showShiftManagement && shiftViewModel != null) {
            Dialog(
                onDismissRequest = { showShiftManagement = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1A1A2E)
                ) {
                    Box {
                        com.dakota.pos.ui.shift.ShiftScreen(
                            viewModel = shiftViewModel,
                            currentUserId = session.userId,
                            currentUserName = session.username
                        )
                        IconButton(
                            onClick = { showShiftManagement = false },
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        ) {
                            Icon(Icons.Default.Close, "Tutup", tint = Color.White)
                        }
                    }
                }
            }
        }

        // Kitchen Display System (KDS) Dialog (Phase 11)
        if (showKds && kdsViewModel != null) {
            Dialog(
                onDismissRequest = { showKds = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                com.dakota.pos.ui.kds.KdsScreen(
                    viewModel = kdsViewModel,
                    onClose = { showKds = false }
                )
            }
        }

        // Finance & Profit Reporting Dialog (Phase 12)
        if (showFinance && financeViewModel != null) {
            Dialog(
                onDismissRequest = { showFinance = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                com.dakota.pos.ui.finance.FinanceScreen(
                    viewModel = financeViewModel,
                    currentUserName = session.username,
                    onClose = { showFinance = false }
                )
            }
        }

        // Staff Attendance & Biometric Payroll Dialog (Phase 13)
        if (showStaffPayroll && staffViewModel != null) {
            Dialog(
                onDismissRequest = { showStaffPayroll = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1A1A2E)
                ) {
                    Box {
                        com.dakota.pos.ui.staff.StaffAttendancePayrollScreen(
                            viewModel = staffViewModel
                        )
                        IconButton(
                            onClick = { showStaffPayroll = false },
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        ) {
                            Icon(Icons.Default.Close, "Tutup", tint = Color.White)
                        }
                    }
                }
            }
        }

        // Snackbar for messages
        uiState.checkoutMessage?.let { message ->
            if (!uiState.showCheckoutSuccess) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp),
                    containerColor = PosSurfaceElevatedDark,
                    contentColor = PosTextPrimaryDark,
                    actionContentColor = CoffeeCaramel,
                    action = {
                        Text(
                            "OK",
                            modifier = Modifier.clickable { viewModel.dismissMessage() },
                            fontWeight = FontWeight.Bold
                        )
                    }
                ) {
                    Text(message)
                }
            }
        }
    }
}

// ============================================================================
//  TOP BAR
// ============================================================================

@Composable
private fun PosTopBar(
    session: UserSession,
    canViewHpp: Boolean,
    onOpenRecipeManagement: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenPurchasing: () -> Unit,
    onOpenShiftManagement: () -> Unit,
    onOpenKds: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenStaffPayroll: () -> Unit = {},
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(PosSurfaceDark, PosSurfaceElevatedDark)
                )
            )
            .border(1.dp, PosBorderDark, RoundedCornerShape(14.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "DAKOTA",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                ),
                color = CoffeeCaramel
            )
            Text(
                text = " POS",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp
                ),
                color = CoffeeLatte
            )
            Spacer(modifier = Modifier.width(16.dp))
            RoleBadge(role = session.role)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Feature action buttons with horizontal scroll
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .horizontalScroll(rememberScrollState())
            ) {
                // KDS Barista button (Phase 11)
                OutlinedButton(
                    onClick = onOpenKds,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("KDS Barista", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(modifier = Modifier.width(8.dp))

                // Shift & Cash Management button (accessible by all cashiers & managers)
                OutlinedButton(
                    onClick = onOpenShiftManagement,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF00C896).copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00C896)),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shift & Kas", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(modifier = Modifier.width(8.dp))

                if (canViewHpp) {
                    OutlinedButton(
                        onClick = onOpenFinance,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Laba & Keuangan", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    // Staff Attendance & Payroll button (Phase 13)
                    OutlinedButton(
                        onClick = onOpenStaffPayroll,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA855F7)),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Staf & Payroll", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (canViewHpp) {
                    OutlinedButton(
                        onClick = onOpenRecipeManagement,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CoffeeCaramel.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CoffeeCaramel),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resep & HPP", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (canViewHpp) {
                    OutlinedButton(
                        onClick = onOpenInventory,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF448AFF).copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF448AFF)),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Inventory", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (canViewHpp) {
                    OutlinedButton(
                        onClick = onOpenPurchasing,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD54F)),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pengadaan", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = session.username,
                style = MaterialTheme.typography.bodyMedium,
                color = PosTextSecondaryDark
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onLogout,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CoffeeEspressoDark)
                    .border(1.dp, CoffeeWarmBrown.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .size(40.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout",
                    tint = CoffeeCaramel,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ============================================================================
//  CATEGORY TABS
// ============================================================================

@Composable
private fun CategoryTabRow(
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelectCategory: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(categories) { category ->
            val isSelected = category.id == selectedCategoryId
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) CoffeeCaramel else PosSurfaceDark,
                label = "cat_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) CoffeeEspressoDark else PosTextSecondaryDark,
                label = "cat_text"
            )

            FilterChip(
                selected = isSelected,
                onClick = { onSelectCategory(category.id) },
                label = {
                    Text(
                        text = category.name,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = textColor
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = PosSurfaceDark,
                    selectedContainerColor = CoffeeCaramel,
                    labelColor = PosTextSecondaryDark,
                    selectedLabelColor = CoffeeEspressoDark
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = PosBorderDark,
                    selectedBorderColor = CoffeeCaramel,
                    enabled = true,
                    selected = isSelected
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

// ============================================================================
//  PRODUCT GRID
// ============================================================================

@Composable
private fun ProductGrid(
    products: List<Product>,
    onProductTap: (Product) -> Unit
) {
    if (products.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("☕", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Belum ada produk",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PosTextMutedDark
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(products, key = { it.id }) { product ->
                ProductCard(product = product, onClick = { onProductTap(product) })
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "card_scale"
    )

    val productColor = try {
        Color(android.graphics.Color.parseColor(product.colorHex))
    } catch (_: Exception) {
        CoffeeWarmBrown
    }

    Card(
        modifier = Modifier
            .scale(scale)
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PosSurfaceElevatedDark),
        border = BorderStroke(1.dp, PosBorderDark)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Color accent bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(productColor, productColor.copy(alpha = 0.4f))
                        )
                    )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = PosTextPrimaryDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (product.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = PosTextMutedDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = formatRupiah(product.basePrice),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = CoffeeCaramel
            )
        }
    }
}

// ============================================================================
//  CART PANEL (Right Side)
// ============================================================================

@Composable
private fun CartPanel(
    cart: com.dakota.pos.domain.model.Cart,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PosSurfaceDark)
            .border(1.dp, PosBorderDark, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Cart Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = CoffeeCaramel,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Keranjang",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PosTextPrimaryDark
                )
                if (cart.totalItemCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(CoffeeCaramel)
                            .size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${cart.totalItemCount}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = CoffeeEspressoDark
                        )
                    }
                }
            }

            if (cart.items.isNotEmpty()) {
                IconButton(
                    onClick = onClearCart,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Hapus Semua",
                        tint = StatusError.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = PosBorderDark, thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        // Cart Items
        if (cart.items.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🛒", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Belum ada item",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PosTextMutedDark,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Tap produk untuk menambahkan",
                        style = MaterialTheme.typography.bodySmall,
                        color = PosTextMutedDark.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cart.items, key = { it.id }) { cartItem ->
                    CartItemRow(
                        item = cartItem,
                        onIncrease = { onIncrease(cartItem.id) },
                        onDecrease = { onDecrease(cartItem.id) },
                        onRemove = { onRemove(cartItem.id) }
                    )
                }
            }
        }

        // Cart Summary + Checkout
        if (cart.items.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = PosBorderDark, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Subtotal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = PosTextSecondaryDark)
                Text(formatRupiah(cart.subtotal), style = MaterialTheme.typography.bodyMedium, color = PosTextPrimaryDark)
            }

            if (cart.discount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Diskon", style = MaterialTheme.typography.bodyMedium, color = StatusError)
                    Text("-${formatRupiah(cart.discount)}", style = MaterialTheme.typography.bodyMedium, color = StatusError)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "TOTAL",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CoffeeCaramel
                )
                Text(
                    formatRupiah(cart.total),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CoffeeCaramel
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCheckout,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoffeeCaramel)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = CoffeeEspressoDark,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Proses Pesanan",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = CoffeeEspressoDark
                )
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PosSurfaceElevatedDark)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.product.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = PosTextPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.summaryDescription.isNotBlank()) {
                Text(
                    text = item.summaryDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = PosTextMutedDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatRupiah(item.subtotal),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = CoffeeCaramel
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Qty controls
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (item.quantity > 1) onDecrease() else onRemove() },
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(PosSurfaceDark)
            ) {
                Icon(
                    if (item.quantity > 1) Icons.Default.Remove else Icons.Default.Delete,
                    contentDescription = "Kurangi",
                    tint = if (item.quantity > 1) PosTextSecondaryDark else StatusError,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = "${item.quantity}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = PosTextPrimaryDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(32.dp)
            )

            IconButton(
                onClick = onIncrease,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(CoffeeCaramel.copy(alpha = 0.2f))
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Tambah",
                    tint = CoffeeCaramel,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ============================================================================
//  PRODUCT DETAIL DIALOG (Variant + Modifier Selection)
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductDetailDialog(
    product: Product,
    variants: List<com.dakota.pos.domain.model.ProductVariant>,
    modifiers: List<com.dakota.pos.domain.model.Modifier>,
    selectedVariant: com.dakota.pos.domain.model.ProductVariant?,
    selectedModifiers: List<com.dakota.pos.domain.model.Modifier>,
    quantity: Int,
    notes: String,
    onSelectVariant: (com.dakota.pos.domain.model.ProductVariant) -> Unit,
    onToggleModifier: (com.dakota.pos.domain.model.Modifier) -> Unit,
    onSetQuantity: (Int) -> Unit,
    onSetNotes: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = PosSurfaceDark,
            border = BorderStroke(1.dp, PosBorderDark)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = PosTextPrimaryDark
                        )
                        if (product.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = product.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PosTextSecondaryDark
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mulai dari ${formatRupiah(product.basePrice)}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = CoffeeCaramel
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = PosTextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = PosBorderDark)

                // Scrollable content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // Variants
                    if (variants.isNotEmpty()) {
                        item {
                            Text(
                                "Pilih Varian",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = CoffeeLatte
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                variants.forEach { variant ->
                                    val isSelected = variant.id == selectedVariant?.id
                                    val chipBg by animateColorAsState(
                                        if (isSelected) CoffeeCaramel else PosSurfaceElevatedDark,
                                        label = "var_bg"
                                    )
                                    val chipText by animateColorAsState(
                                        if (isSelected) CoffeeEspressoDark else PosTextSecondaryDark,
                                        label = "var_text"
                                    )

                                    Surface(
                                        modifier = Modifier.clickable { onSelectVariant(variant) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = chipBg,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) CoffeeCaramel else PosBorderDark
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                variant.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = chipText
                                            )
                                            if (variant.priceAdjustment != 0.0) {
                                                Text(
                                                    "+${formatRupiah(variant.priceAdjustment)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = chipText.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Modifiers grouped
                    val groupedModifiers = modifiers.groupBy { it.groupName }
                    if (groupedModifiers.isNotEmpty()) {
                        groupedModifiers.forEach { (groupName, groupMods) ->
                            item {
                                Text(
                                    groupName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = CoffeeLatte
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    groupMods.forEach { mod ->
                                        val isChecked = selectedModifiers.any { it.id == mod.id }
                                        FilterChip(
                                            selected = isChecked,
                                            onClick = { onToggleModifier(mod) },
                                            label = {
                                                Text(
                                                    if (mod.price > 0) "${mod.name} (+${formatRupiah(mod.price)})"
                                                    else mod.name,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = PosSurfaceElevatedDark,
                                                selectedContainerColor = CoffeeWarmBrown.copy(alpha = 0.3f),
                                                labelColor = PosTextSecondaryDark,
                                                selectedLabelColor = CoffeeCream
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                borderColor = PosBorderDark,
                                                selectedBorderColor = CoffeeCaramel,
                                                enabled = true,
                                                selected = isChecked
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Notes
                    item {
                        Text(
                            "Catatan",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = CoffeeLatte
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = onSetNotes,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Contoh: less ice, extra hot...", color = PosTextMutedDark) },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedTextColor = PosTextPrimaryDark,
                                focusedTextColor = PosTextPrimaryDark,
                                cursorColor = CoffeeCaramel,
                                focusedBorderColor = CoffeeCaramel,
                                unfocusedBorderColor = PosBorderDark,
                                unfocusedContainerColor = PosSurfaceElevatedDark,
                                focusedContainerColor = PosSurfaceElevatedDark
                            ),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 2
                        )
                    }
                }

                HorizontalDivider(color = PosBorderDark)
                Spacer(modifier = Modifier.height(16.dp))

                // Bottom: Quantity + Price + Add button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Quantity
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onSetQuantity(quantity - 1) },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PosSurfaceElevatedDark)
                                .border(1.dp, PosBorderDark, RoundedCornerShape(8.dp)),
                            enabled = quantity > 1
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Kurangi", tint = PosTextSecondaryDark)
                        }

                        Text(
                            text = "$quantity",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PosTextPrimaryDark,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(48.dp)
                        )

                        IconButton(
                            onClick = { onSetQuantity(quantity + 1) },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CoffeeCaramel.copy(alpha = 0.2f))
                                .border(1.dp, CoffeeCaramel.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah", tint = CoffeeCaramel)
                        }
                    }

                    // Computed price
                    val computedUnitPrice = product.basePrice +
                            (selectedVariant?.priceAdjustment ?: 0.0) +
                            selectedModifiers.sumOf { it.price }
                    val computedTotal = computedUnitPrice * quantity

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${formatRupiah(computedUnitPrice)} × $quantity",
                            style = MaterialTheme.typography.bodySmall,
                            color = PosTextSecondaryDark
                        )
                        Text(
                            formatRupiah(computedTotal),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = CoffeeCaramel
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoffeeCaramel),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = CoffeeEspressoDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Tambah",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = CoffeeEspressoDark
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
//  CHECKOUT SUCCESS OVERLAY
// ============================================================================

@Composable
private fun CheckoutSuccessOverlay(
    order: com.dakota.pos.domain.model.Order,
    paymentResult: com.dakota.pos.domain.payment.PaymentResult.Success?,
    printStatusMessage: String?,
    onPrintReceipt: () -> Unit,
    onOpenDrawer: () -> Unit,
    onShowPreview: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PosBackgroundDark.copy(alpha = 0.85f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* Block clicks */ },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PosSurfaceDark,
                border = BorderStroke(2.dp, StatusSuccess.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(0.48f)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusSuccess,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Pembayaran Berhasil!",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = PosTextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        order.orderNumber,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = CoffeeCaramel
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Detail ringkasan pembayaran
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PosSurfaceElevatedDark,
                        border = BorderStroke(1.dp, PosBorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Tagihan", color = PosTextSecondaryDark, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    formatRupiah(order.total),
                                    color = PosTextPrimaryDark,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Metode Bayar", color = PosTextSecondaryDark, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    order.paymentMethod ?: "CASH",
                                    color = CoffeeCaramel,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            if (paymentResult != null && paymentResult.change > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Kembalian", color = StatusSuccess, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        formatRupiah(paymentResult.change),
                                        color = StatusSuccess,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            if (paymentResult != null && !paymentResult.transactionReference.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Ref Transaksi", color = PosTextSecondaryDark, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        paymentResult.transactionReference!!,
                                        color = PosTextSecondaryDark,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                            }
                        }
                    }

                    if (!printStatusMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = printStatusMessage,
                            color = StatusSuccess,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Secondary action buttons (Print, Preview, Drawer)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPrintReceipt,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CoffeeCaramel.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoffeeCaramel),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cetak Struk", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }

                        OutlinedButton(
                            onClick = onShowPreview,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, PosBorderDark),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PosTextSecondaryDark),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Preview", style = MaterialTheme.typography.bodySmall)
                        }

                        OutlinedButton(
                            onClick = onOpenDrawer,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, PosBorderDark),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PosTextSecondaryDark),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Laci", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Primary Action: Pesanan Baru
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoffeeCaramel),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            "Pesanan Baru",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = CoffeeEspressoDark
                        )
                    }
                }
            }
        }
    }
}
