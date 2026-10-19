package com.dakota.pos.ui.purchasing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dakota.pos.domain.model.purchasing.GoodsReceipt
import com.dakota.pos.domain.model.purchasing.PurchaseOrder
import com.dakota.pos.domain.model.purchasing.PurchaseOrderItem
import com.dakota.pos.domain.model.purchasing.PurchaseOrderStatus
import com.dakota.pos.domain.model.purchasing.Supplier
import com.dakota.pos.domain.model.recipe.Ingredient
import com.dakota.pos.domain.model.unit.UnitType
import com.dakota.pos.domain.usecase.purchasing.IncomingItemInput
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// ─── Color Palette ──────────────────────────────────────────────────────────
private val DarkSurface = Color(0xFF1A1A2E)
private val DarkCard = Color(0xFF16213E)
private val DarkCardElevated = Color(0xFF1F2B4C)
private val AccentGreen = Color(0xFF00C896)
private val AccentRed = Color(0xFFFF5252)
private val AccentOrange = Color(0xFFFFAB40)
private val AccentBlue = Color(0xFF448AFF)
private val AccentPurple = Color(0xFF7C4DFF)
private val AccentGold = Color(0xFFFFD54F)
private val TextPrimary = Color(0xFFE0E0E0)
private val TextSecondary = Color(0xFF9E9E9E)

private val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
    maximumFractionDigits = 0
}
private fun formatRupiah(amount: Double): String = rupiahFormat.format(amount)

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    return sdf.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasingScreen(
    viewModel: PurchasingViewModel,
    currentUserName: String = "Admin"
) {
    val uiState by viewModel.uiState.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val purchaseOrders by viewModel.purchaseOrders.collectAsState()
    val goodsReceipts by viewModel.goodsReceipts.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkSurface,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🛒 Pengadaan & Supplier", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Phase 09: Purchasing",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentGold,
                            modifier = Modifier
                                .background(AccentGold.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCard),
                actions = {
                    if (uiState.activeTab == PurchasingTab.SUPPLIERS) {
                        IconButton(onClick = { viewModel.openSupplierDialog() }) {
                            Icon(Icons.Default.Add, "Tambah Supplier", tint = AccentGreen)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = uiState.activeTab.ordinal,
                containerColor = DarkCard,
                contentColor = AccentBlue
            ) {
                Tab(
                    selected = uiState.activeTab == PurchasingTab.PO_LIST,
                    onClick = { viewModel.setActiveTab(PurchasingTab.PO_LIST) },
                    text = { Text("Daftar PO (${purchaseOrders.size})", color = if (uiState.activeTab == PurchasingTab.PO_LIST) AccentBlue else TextSecondary) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = if (uiState.activeTab == PurchasingTab.PO_LIST) AccentBlue else TextSecondary) }
                )
                Tab(
                    selected = uiState.activeTab == PurchasingTab.CREATE_PO,
                    onClick = { viewModel.setActiveTab(PurchasingTab.CREATE_PO) },
                    text = { Text("Buat PO", color = if (uiState.activeTab == PurchasingTab.CREATE_PO) AccentBlue else TextSecondary) },
                    icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = if (uiState.activeTab == PurchasingTab.CREATE_PO) AccentBlue else TextSecondary) }
                )
                Tab(
                    selected = uiState.activeTab == PurchasingTab.GOODS_RECEIPTS,
                    onClick = { viewModel.setActiveTab(PurchasingTab.GOODS_RECEIPTS) },
                    text = { Text("Penerimaan (${goodsReceipts.size})", color = if (uiState.activeTab == PurchasingTab.GOODS_RECEIPTS) AccentBlue else TextSecondary) },
                    icon = { Icon(Icons.Default.LocalShipping, contentDescription = null, tint = if (uiState.activeTab == PurchasingTab.GOODS_RECEIPTS) AccentBlue else TextSecondary) }
                )
                Tab(
                    selected = uiState.activeTab == PurchasingTab.SUPPLIERS,
                    onClick = { viewModel.setActiveTab(PurchasingTab.SUPPLIERS) },
                    text = { Text("Pemasok (${suppliers.size})", color = if (uiState.activeTab == PurchasingTab.SUPPLIERS) AccentBlue else TextSecondary) },
                    icon = { Icon(Icons.Default.Store, contentDescription = null, tint = if (uiState.activeTab == PurchasingTab.SUPPLIERS) AccentBlue else TextSecondary) }
                )
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (uiState.activeTab) {
                    PurchasingTab.PO_LIST -> PoListContent(
                        purchaseOrders = purchaseOrders,
                        selectedStatus = uiState.selectedStatusFilter,
                        onSelectStatus = { viewModel.setStatusFilter(it) },
                        onSubmitPo = { viewModel.submitPurchaseOrder(it) },
                        onReceivePo = { viewModel.openReceiveDialog(it) },
                        onDetailPo = { viewModel.openPoDetail(it) },
                        onCreatePoClick = { viewModel.setActiveTab(PurchasingTab.CREATE_PO) }
                    )
                    PurchasingTab.CREATE_PO -> CreatePoContent(
                        suppliers = suppliers,
                        ingredients = ingredients,
                        currentUserName = currentUserName,
                        onCreatePo = { supId, items, notes, submit ->
                            viewModel.createPurchaseOrder(supId, items, notes, currentUserName, submit)
                        },
                        onOpenAddSupplier = { viewModel.openSupplierDialog() }
                    )
                    PurchasingTab.GOODS_RECEIPTS -> GoodsReceiptsContent(
                        receipts = goodsReceipts
                    )
                    PurchasingTab.SUPPLIERS -> SuppliersContent(
                        suppliers = suppliers,
                        onAddSupplier = { viewModel.openSupplierDialog() },
                        onEditSupplier = { viewModel.openSupplierDialog(it) },
                        onDeleteSupplier = { viewModel.deleteSupplier(it) }
                    )
                }
            }
        }
    }

    // Dialogs
    if (uiState.showSupplierDialog) {
        SupplierDialog(
            supplier = uiState.editingSupplier,
            onDismiss = { viewModel.closeSupplierDialog() },
            onSave = { name, contact, phone, email, addr, terms ->
                viewModel.saveSupplier(name, contact, phone, email, addr, terms)
            }
        )
    }

    if (uiState.showReceiveDialog && uiState.selectedPoForReceiving != null) {
        ReceiveGoodsDialog(
            po = uiState.selectedPoForReceiving!!,
            onDismiss = { viewModel.closeReceiveDialog() },
            onConfirmReceive = { items, notes ->
                viewModel.processGoodsReceipt(
                    poId = uiState.selectedPoForReceiving!!.id,
                    receivedBy = currentUserName,
                    items = items,
                    notes = notes
                )
            }
        )
    }

    if (uiState.showPoDetailDialog && uiState.selectedPoForDetail != null) {
        PoDetailDialog(
            po = uiState.selectedPoForDetail!!,
            onDismiss = { viewModel.closePoDetail() },
            onReceiveClick = {
                viewModel.closePoDetail()
                viewModel.openReceiveDialog(it)
            }
        )
    }
}

// ============================================================================
// 1. PO LIST CONTENT
// ============================================================================

@Composable
private fun PoListContent(
    purchaseOrders: List<PurchaseOrder>,
    selectedStatus: PurchaseOrderStatus?,
    onSelectStatus: (PurchaseOrderStatus?) -> Unit,
    onSubmitPo: (String) -> Unit,
    onReceivePo: (PurchaseOrder) -> Unit,
    onDetailPo: (PurchaseOrder) -> Unit,
    onCreatePoClick: () -> Unit
) {
    val filtered = if (selectedStatus == null) {
        purchaseOrders
    } else {
        purchaseOrders.filter { it.status == selectedStatus }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Status Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedStatus == null,
                onClick = { onSelectStatus(null) },
                label = { Text("Semua (${purchaseOrders.size})") }
            )
            PurchaseOrderStatus.entries.forEach { status ->
                val count = purchaseOrders.count { it.status == status }
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = { onSelectStatus(status) },
                    label = { Text("${status.name} ($count)") }
                )
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Belum ada Purchase Order", color = TextSecondary, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onCreatePoClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buat Purchase Order Baru")
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtered) { po ->
                    PurchaseOrderCard(
                        po = po,
                        onSubmit = { onSubmitPo(po.id) },
                        onReceive = { onReceivePo(po) },
                        onDetail = { onDetailPo(po) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PurchaseOrderCard(
    po: PurchaseOrder,
    onSubmit: () -> Unit,
    onReceive: () -> Unit,
    onDetail: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetail() },
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: PO Number + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = po.poNumber,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• " + formatDate(po.createdAt),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                PoStatusBadge(status = po.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body: Supplier & Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Supplier: ${po.supplierName}",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${po.items.size} item bahan baku",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = formatRupiah(po.totalAmount),
                    color = AccentGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Progress bar penerimaan barang jika SUBMITTED atau RECEIVED
            if (po.status == PurchaseOrderStatus.SUBMITTED || po.status == PurchaseOrderStatus.RECEIVED) {
                val totalOrdered = po.items.sumOf { it.orderedQuantity }
                val totalReceived = po.items.sumOf { it.receivedQuantity }
                val progress = if (totalOrdered > 0) (totalReceived / totalOrdered).toFloat().coerceIn(0f, 1f) else 0f

                Spacer(modifier = Modifier.height(10.dp))
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Penerimaan: ${String.format(Locale.US, "%.1f", totalReceived)} / ${String.format(Locale.US, "%.1f", totalOrdered)} item",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 11.sp,
                            color = if (progress >= 1f) AccentGreen else AccentOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = if (progress >= 1f) AccentGreen else AccentOrange,
                        trackColor = DarkSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDetail,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Detail", fontSize = 12.sp)
                }

                if (po.status == PurchaseOrderStatus.DRAFT) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSubmit,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Submit PO", fontSize = 12.sp)
                    }
                }

                if (po.status == PurchaseOrderStatus.SUBMITTED || po.status == PurchaseOrderStatus.RECEIVED) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onReceive,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Terima Barang", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PoStatusBadge(status: PurchaseOrderStatus) {
    val (bgColor, textColor) = when (status) {
        PurchaseOrderStatus.DRAFT -> Color(0xFF455A64) to Color(0xFFCFD8DC)
        PurchaseOrderStatus.SUBMITTED -> AccentBlue.copy(alpha = 0.2f) to AccentBlue
        PurchaseOrderStatus.RECEIVED -> AccentOrange.copy(alpha = 0.2f) to AccentOrange
        PurchaseOrderStatus.COMPLETED -> AccentGreen.copy(alpha = 0.2f) to AccentGreen
        PurchaseOrderStatus.CANCELLED -> AccentRed.copy(alpha = 0.2f) to AccentRed
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = status.name,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ============================================================================
// 2. CREATE PO CONTENT
// ============================================================================

@Composable
private fun CreatePoContent(
    suppliers: List<Supplier>,
    ingredients: List<Ingredient>,
    currentUserName: String,
    onCreatePo: (String, List<PurchaseOrderItem>, String?, Boolean) -> Unit,
    onOpenAddSupplier: () -> Unit
) {
    var selectedSupplierId by remember { mutableStateOf(suppliers.firstOrNull()?.id ?: "") }
    var notes by remember { mutableStateOf("") }

    // Draft items
    var poItems by remember { mutableStateOf(listOf<PurchaseOrderItem>()) }

    // Add item picker state
    var selectedIngredientId by remember { mutableStateOf(ingredients.firstOrNull()?.id ?: "") }
    var itemQtyText by remember { mutableStateOf("1") }
    var itemPriceText by remember { mutableStateOf("") }

    val totalAmount = poItems.sumOf { it.subtotal }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("1. Pilih Supplier", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (suppliers.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Belum ada supplier.", color = AccentRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = onOpenAddSupplier) {
                                Text("+ Tambah Supplier")
                            }
                        }
                    } else {
                        // Radio / List of suppliers
                        suppliers.forEach { sup ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSupplierId = sup.id }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedSupplierId == sup.id,
                                    onClick = { selectedSupplierId = sup.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = AccentBlue)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(sup.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text("${sup.phone} • Termin: ${sup.paymentTerms}", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("2. Tambah Bahan Baku yang Dipesan", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (ingredients.isEmpty()) {
                        Text("Belum ada data bahan baku di master database.", color = TextSecondary)
                    } else {
                        val activeIngredient = ingredients.find { it.id == selectedIngredientId } ?: ingredients.first()

                        LaunchedEffect(selectedIngredientId) {
                            if (itemPriceText.isBlank() || itemPriceText == "0") {
                                itemPriceText = activeIngredient.costPerDefaultUnit.toInt().toString()
                            }
                        }

                        // Select Ingredient Dropdown / Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Quick Ingredient selector chips
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(max = 140.dp)
                            ) {
                                items(ingredients) { ing ->
                                    val isSel = selectedIngredientId == ing.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isSel) AccentBlue.copy(alpha = 0.2f) else Color.Transparent,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable {
                                                selectedIngredientId = ing.id
                                                itemPriceText = ing.costPerDefaultUnit.toInt().toString()
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(ing.name, color = if (isSel) AccentBlue else TextPrimary, fontSize = 13.sp)
                                        Text(
                                            "Stok: ${ing.currentStock} ${ing.defaultUnit.symbol} • HPP: ${formatRupiah(ing.costPerDefaultUnit)}",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = itemQtyText,
                                onValueChange = { itemQtyText = it },
                                label = { Text("Jumlah (${activeIngredient.defaultUnit.symbol})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            OutlinedTextField(
                                value = itemPriceText,
                                onValueChange = { itemPriceText = it },
                                label = { Text("Harga Satuan (Rp)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.5f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Button(
                                onClick = {
                                    val qty = itemQtyText.toDoubleOrNull() ?: 0.0
                                    val price = itemPriceText.toDoubleOrNull() ?: 0.0
                                    if (qty > 0 && price >= 0) {
                                        val newItem = PurchaseOrderItem(
                                            id = UUID.randomUUID().toString(),
                                            purchaseOrderId = "",
                                            ingredientId = activeIngredient.id,
                                            ingredientName = activeIngredient.name,
                                            unit = activeIngredient.defaultUnit,
                                            orderedQuantity = qty,
                                            unitPrice = price,
                                            subtotal = qty * price
                                        )
                                        poItems = poItems + newItem
                                        itemQtyText = "1"
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                modifier = Modifier.height(54.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tambah")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Added items list
                    Text("Daftar Item PO (${poItems.size}):", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (poItems.isEmpty()) {
                        Text("Belum ada item ditambahkan.", color = TextSecondary, fontSize = 12.sp)
                    } else {
                        poItems.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkCardElevated, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.ingredientName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        "${item.orderedQuantity} ${item.unit.symbol} @ ${formatRupiah(item.unitPrice)}",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    formatRupiah(item.subtotal),
                                    color = AccentGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                IconButton(
                                    onClick = { poItems = poItems.filterIndexed { i, _ -> i != index } }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = AccentRed)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("3. Catatan & Konfirmasi", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan PO (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Pembelian:", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(formatRupiah(totalAmount), color = AccentGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (selectedSupplierId.isNotBlank() && poItems.isNotEmpty()) {
                                    onCreatePo(selectedSupplierId, poItems, notes, false)
                                }
                            },
                            enabled = selectedSupplierId.isNotBlank() && poItems.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simpan Draft")
                        }

                        Button(
                            onClick = {
                                if (selectedSupplierId.isNotBlank() && poItems.isNotEmpty()) {
                                    onCreatePo(selectedSupplierId, poItems, notes, true)
                                }
                            },
                            enabled = selectedSupplierId.isNotBlank() && poItems.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Submit PO Sekarang")
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 3. GOODS RECEIPTS CONTENT
// ============================================================================

@Composable
private fun GoodsReceiptsContent(
    receipts: List<GoodsReceipt>
) {
    if (receipts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Belum ada riwayat penerimaan barang (Goods Receipt)", color = TextSecondary, fontSize = 16.sp)
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(receipts) { receipt ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(receipt.receiptNumber, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            Text(formatDate(receipt.timestamp), color = TextSecondary, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Ref: PO ${receipt.poNumber} • ${receipt.supplierName}", color = AccentBlue, fontSize = 13.sp)
                        Text("Diterima oleh: ${receipt.receivedBy}", color = TextSecondary, fontSize = 12.sp)

                        if (!receipt.notes.isNullOrBlank()) {
                            Text("Catatan: ${receipt.notes}", color = TextSecondary, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = DarkSurface)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Item Diterima & Masuk Stok:", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        receipt.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• ${item.ingredientName}", color = TextPrimary, fontSize = 13.sp)
                                Text(
                                    "+${item.receivedQuantity} ${item.unit.symbol} @ ${formatRupiah(item.unitPrice)}",
                                    color = AccentGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. SUPPLIERS CONTENT
// ============================================================================

@Composable
private fun SuppliersContent(
    suppliers: List<Supplier>,
    onAddSupplier: () -> Unit,
    onEditSupplier: (Supplier) -> Unit,
    onDeleteSupplier: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Daftar Mitra Pemasok", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 18.sp)
            Button(
                onClick = onAddSupplier,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tambah Supplier")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (suppliers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Belum ada data supplier.", color = TextSecondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(suppliers) { sup ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(sup.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                Surface(
                                    color = AccentPurple.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        sup.paymentTerms,
                                        color = AccentPurple,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            sup.contactPerson?.let {
                                Text("PIC: $it", color = TextSecondary, fontSize = 13.sp)
                            }
                            Text("Telepon: ${sup.phone}", color = TextPrimary, fontSize = 13.sp)
                            sup.email?.let {
                                Text("Email: $it", color = TextSecondary, fontSize = 13.sp)
                            }
                            sup.address?.let {
                                Text("Alamat: $it", color = TextSecondary, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { onEditSupplier(sup) }) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentBlue)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit", color = AccentBlue)
                                }
                                TextButton(onClick = { onDeleteSupplier(sup.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentRed)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Hapus", color = AccentRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// DIALOGS: RECEIVE GOODS, SUPPLIER, PO DETAIL
// ============================================================================

@Composable
fun ReceiveGoodsDialog(
    po: PurchaseOrder,
    onDismiss: () -> Unit,
    onConfirmReceive: (List<IncomingItemInput>, String?) -> Unit
) {
    // Map to hold input received quantity for each item
    val inputQuantities = remember(po) {
        mutableStateMapOf<String, String>().apply {
            po.items.forEach { item ->
                // Default input to remaining quantity
                val rem = item.remainingQuantity
                put(item.id, if (rem > 0) rem.toString() else "0")
            }
        }
    }
    var receiptNotes by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = DarkCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("📦 Form Penerimaan Barang (Goods Receipt)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("Nomor PO: ${po.poNumber} • Supplier: ${po.supplierName}", color = AccentBlue, fontSize = 13.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Masukkan kuantitas fisik bahan baku yang datang. Sistem akan otomatis menambah stok dan memperbarui HPP via Moving Average.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Items list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(po.items) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.ingredientName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    Text(
                                        "Dipesan: ${item.orderedQuantity} ${item.unit.symbol} | Sudah diterima: ${item.receivedQuantity} ${item.unit.symbol}",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "Sisa belum datang: ${item.remainingQuantity} ${item.unit.symbol}",
                                        color = if (item.remainingQuantity > 0) AccentOrange else AccentGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                OutlinedTextField(
                                    value = inputQuantities[item.id] ?: "0",
                                    onValueChange = { inputQuantities[item.id] = it },
                                    label = { Text("Diterima (${item.unit.symbol})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(140.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = receiptNotes,
                    onValueChange = { receiptNotes = it },
                    label = { Text("Catatan Penerimaan / Surat Jalan (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val items = po.items.mapNotNull { item ->
                                val qty = inputQuantities[item.id]?.toDoubleOrNull() ?: 0.0
                                if (qty > 0) {
                                    IncomingItemInput(
                                        purchaseOrderItemId = item.id,
                                        quantityReceived = qty
                                    )
                                } else null
                            }
                            if (items.isNotEmpty()) {
                                onConfirmReceive(items, receiptNotes)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Konfirmasi & Tambah Stok")
                    }
                }
            }
        }
    }
}

@Composable
fun SupplierDialog(
    supplier: Supplier?,
    onDismiss: () -> Unit,
    onSave: (String, String?, String, String?, String?, String) -> Unit
) {
    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var contact by remember { mutableStateOf(supplier?.contactPerson ?: "") }
    var phone by remember { mutableStateOf(supplier?.phone ?: "") }
    var email by remember { mutableStateOf(supplier?.email ?: "") }
    var address by remember { mutableStateOf(supplier?.address ?: "") }
    var terms by remember { mutableStateOf(supplier?.paymentTerms ?: "CASH") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkCard,
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = if (supplier == null) "Tambah Supplier Baru" else "Edit Supplier",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Perusahaan / Supplier *") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Nama PIC / Kontak Person") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Nomor Telepon / WA *") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Payment Terms Row
                Text("Termin Pembayaran:", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("CASH", "NET_7", "NET_14", "NET_30").forEach { t ->
                        FilterChip(
                            selected = terms == t,
                            onClick = { terms = t },
                            label = { Text(t) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onSave(name, contact, phone, email, address, terms) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun PoDetailDialog(
    po: PurchaseOrder,
    onDismiss: () -> Unit,
    onReceiveClick: (PurchaseOrder) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = DarkCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Detail Purchase Order", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text(po.poNumber, color = AccentBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    PoStatusBadge(status = po.status)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Supplier: ${po.supplierName}", color = TextPrimary, fontSize = 14.sp)
                Text("Tanggal Pesan: ${formatDate(po.createdAt)}", color = TextSecondary, fontSize = 13.sp)
                Text("Dibuat oleh: ${po.createdBy}", color = TextSecondary, fontSize = 13.sp)
                if (!po.notes.isNullOrBlank()) {
                    Text("Catatan: ${po.notes}", color = TextSecondary, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Rincian Bahan Baku:", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(po.items) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(item.ingredientName, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(
                                        "Pesan: ${item.orderedQuantity} ${item.unit.symbol} @ ${formatRupiah(item.unitPrice)}",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "Diterima: ${item.receivedQuantity} / ${item.orderedQuantity} ${item.unit.symbol}",
                                        color = if (item.isFullyReceived) AccentGreen else AccentOrange,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    formatRupiah(item.subtotal),
                                    color = AccentGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total PO:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    Text(formatRupiah(po.totalAmount), fontWeight = FontWeight.Bold, color = AccentGold, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                        Text("Tutup")
                    }
                    if (po.status == PurchaseOrderStatus.SUBMITTED || po.status == PurchaseOrderStatus.RECEIVED) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { onReceiveClick(po) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Terima Barang")
                        }
                    }
                }
            }
        }
    }
}
