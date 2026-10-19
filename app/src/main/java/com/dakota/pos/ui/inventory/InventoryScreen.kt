package com.dakota.pos.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dakota.pos.domain.model.inventory.MovementType
import com.dakota.pos.domain.model.inventory.StockMovement
import com.dakota.pos.domain.model.recipe.Ingredient
import java.text.SimpleDateFormat
import java.util.*

// ─── Color Palette ──────────────────────────────────────────────────────────

private val DarkSurface = Color(0xFF1A1A2E)
private val DarkCard = Color(0xFF16213E)
private val AccentGreen = Color(0xFF00C896)
private val AccentRed = Color(0xFFFF5252)
private val AccentOrange = Color(0xFFFFAB40)
private val AccentBlue = Color(0xFF448AFF)
private val AccentPurple = Color(0xFF7C4DFF)
private val TextPrimary = Color(0xFFE0E0E0)
private val TextSecondary = Color(0xFF9E9E9E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()
    val movements by viewModel.recentMovements.collectAsState()

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
                    Text("📦 Inventory Management", color = TextPrimary, fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCard),
                actions = {
                    IconButton(onClick = { viewModel.showOpnameDialog() }) {
                        Icon(Icons.Filled.Checklist, "Stock Opname", tint = AccentPurple)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = uiState.activeTab.ordinal,
                containerColor = DarkCard,
                contentColor = AccentBlue
            ) {
                InventoryTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.activeTab == tab,
                        onClick = { viewModel.setActiveTab(tab) },
                        text = {
                            Text(
                                when (tab) {
                                    InventoryTab.MONITORING -> "Monitoring Stok"
                                    InventoryTab.MOVEMENTS -> "Log Mutasi"
                                    InventoryTab.OPNAME -> "Stock Opname"
                                },
                                color = if (uiState.activeTab == tab) AccentBlue else TextSecondary
                            )
                        }
                    )
                }
            }

            // Content
            when (uiState.activeTab) {
                InventoryTab.MONITORING -> StockMonitoringTab(ingredients, viewModel)
                InventoryTab.MOVEMENTS -> MovementLogTab(movements)
                InventoryTab.OPNAME -> StockOpnameTab(ingredients, viewModel)
            }
        }

        // Dialogs
        if (uiState.showStockInDialog && uiState.selectedIngredient != null) {
            StockMovementDialog(
                title = "Stok Masuk",
                ingredient = uiState.selectedIngredient!!,
                onDismiss = { viewModel.dismissDialogs() },
                onConfirm = { qty, notes ->
                    viewModel.performStockIn(
                        uiState.selectedIngredient!!.id, qty, notes,
                        "system", "System"
                    )
                }
            )
        }
        if (uiState.showStockOutDialog && uiState.selectedIngredient != null) {
            StockMovementDialog(
                title = "Stok Keluar",
                ingredient = uiState.selectedIngredient!!,
                onDismiss = { viewModel.dismissDialogs() },
                onConfirm = { qty, notes ->
                    viewModel.performStockOut(
                        uiState.selectedIngredient!!.id, qty, notes,
                        "system", "System"
                    )
                }
            )
        }
        if (uiState.showWastageDialog && uiState.selectedIngredient != null) {
            WastageDialog(
                ingredient = uiState.selectedIngredient!!,
                onDismiss = { viewModel.dismissDialogs() },
                onConfirm = { qty, reason ->
                    viewModel.performWastage(
                        uiState.selectedIngredient!!.id, qty, reason,
                        "system", "System"
                    )
                }
            )
        }
    }
}

// ─── Stock Monitoring Tab ───────────────────────────────────────────────────

@Composable
private fun StockMonitoringTab(
    ingredients: List<Ingredient>,
    viewModel: InventoryViewModel
) {
    if (ingredients.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada data bahan baku", color = TextSecondary, fontSize = 16.sp)
        }
        return
    }

    // Summary Cards
    val lowStockCount = ingredients.count { it.currentStock <= it.minimumStock && it.minimumStock > 0 }
    val totalIngredients = ingredients.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Summary Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Bahan",
                    value = "$totalIngredients",
                    color = AccentBlue
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Stok Rendah",
                    value = "$lowStockCount",
                    color = if (lowStockCount > 0) AccentRed else AccentGreen
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // Ingredient Stock List
        items(ingredients, key = { it.id }) { ingredient ->
            IngredientStockCard(ingredient = ingredient, viewModel = viewModel)
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun IngredientStockCard(
    ingredient: Ingredient,
    viewModel: InventoryViewModel
) {
    val isLowStock = ingredient.minimumStock > 0 && ingredient.currentStock <= ingredient.minimumStock
    val stockColor = when {
        ingredient.currentStock <= 0 -> AccentRed
        isLowStock -> AccentOrange
        else -> AccentGreen
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(ingredient.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.height(2.dp))
                    Text("SKU: ${ingredient.sku}", color = TextSecondary, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${"%.1f".format(ingredient.currentStock)} ${ingredient.defaultUnit.symbol}",
                        color = stockColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    if (ingredient.minimumStock > 0) {
                        Text(
                            "Min: ${"%.1f".format(ingredient.minimumStock)} ${ingredient.defaultUnit.symbol}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Stock progress bar
            if (ingredient.minimumStock > 0) {
                Spacer(Modifier.height(8.dp))
                val maxDisplay = ingredient.minimumStock * 3
                val progress = (ingredient.currentStock / maxDisplay).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = stockColor,
                    trackColor = Color(0xFF2A2A4A),
                )
            }

            // Low stock warning
            if (isLowStock) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "⚠️ Stok di bawah minimum!",
                    color = AccentOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(8.dp))
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { viewModel.showStockInDialog(ingredient) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AccentGreen.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Add, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Masuk", color = AccentGreen, fontSize = 12.sp)
                }
                FilledTonalButton(
                    onClick = { viewModel.showStockOutDialog(ingredient) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AccentRed.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Remove, null, tint = AccentRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Keluar", color = AccentRed, fontSize = 12.sp)
                }
                FilledTonalButton(
                    onClick = { viewModel.showWastageDialog(ingredient) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AccentOrange.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Delete, null, tint = AccentOrange, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Waste", color = AccentOrange, fontSize = 12.sp)
                }
            }
        }
    }
}

// ─── Movement Log Tab ───────────────────────────────────────────────────────

@Composable
private fun MovementLogTab(movements: List<StockMovement>) {
    if (movements.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada log mutasi stok", color = TextSecondary, fontSize = 16.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(movements, key = { it.id }) { movement ->
            MovementCard(movement)
        }
    }
}

@Composable
private fun MovementCard(movement: StockMovement) {
    val (typeLabel, typeColor) = when (movement.type) {
        MovementType.IN -> "MASUK" to AccentGreen
        MovementType.OUT -> "KELUAR" to AccentRed
        MovementType.SALE -> "PENJUALAN" to AccentBlue
        MovementType.ADJUSTMENT -> "ADJUSTMENT" to AccentPurple
        MovementType.WASTAGE -> "WASTAGE" to AccentOrange
        MovementType.RETURN -> "RETURN" to Color(0xFF26C6DA)
    }

    val sdf = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type Badge
            Surface(
                color = typeColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    typeLabel,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = typeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(movement.ingredientName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Text(
                    movement.notes ?: "",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val changePrefix = if (movement.quantityChange >= 0) "+" else ""
                Text(
                    "$changePrefix${"%.1f".format(movement.quantityChange)} ${movement.unit.symbol}",
                    color = if (movement.quantityChange >= 0) AccentGreen else AccentRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    sdf.format(Date(movement.timestamp)),
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ─── Stock Opname Tab ───────────────────────────────────────────────────────

@Composable
private fun StockOpnameTab(
    ingredients: List<Ingredient>,
    viewModel: InventoryViewModel
) {
    val opnameCounts = remember { mutableStateMapOf<String, String>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Stock Opname",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Masukkan stok fisik aktual untuk setiap bahan baku",
                color = TextSecondary,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(12.dp))
        }

        items(ingredients, key = { it.id }) { ingredient ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ingredient.name, color = TextPrimary, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Sistem: ${"%.1f".format(ingredient.currentStock)} ${ingredient.defaultUnit.symbol}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    OutlinedTextField(
                        value = opnameCounts[ingredient.id] ?: "",
                        onValueChange = { opnameCounts[ingredient.id] = it },
                        modifier = Modifier.width(120.dp),
                        label = { Text("Fisik", color = TextSecondary, fontSize = 12.sp) },
                        suffix = { Text(ingredient.defaultUnit.symbol, color = TextSecondary, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = Color(0xFF2A2A4A)
                        )
                    )
                }
            }
        }

        // Submit Button
        item {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val counts = opnameCounts
                        .filter { it.value.isNotBlank() }
                        .mapValues { it.value.toDoubleOrNull() ?: 0.0 }
                    if (counts.isNotEmpty()) {
                        viewModel.performStockOpname(counts, "system", "System", null)
                        opnameCounts.clear()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Checklist, null)
                Spacer(Modifier.width(8.dp))
                Text("Simpan Stock Opname", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Dialogs ────────────────────────────────────────────────────────────────

@Composable
private fun StockMovementDialog(
    title: String,
    ingredient: Ingredient,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Double, notes: String?) -> Unit
) {
    var quantity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text(title, color = TextPrimary) },
        text = {
            Column {
                Text(
                    ingredient.name,
                    color = AccentBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    "Stok saat ini: ${"%.1f".format(ingredient.currentStock)} ${ingredient.defaultUnit.symbol}",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Jumlah (${ingredient.defaultUnit.symbol})", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = Color(0xFF2A2A4A)
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan (opsional)", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = Color(0xFF2A2A4A)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantity.toDoubleOrNull()
                    if (qty != null && qty > 0) {
                        onConfirm(qty, notes.ifBlank { null })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = TextSecondary) }
        }
    )
}

@Composable
private fun WastageDialog(
    ingredient: Ingredient,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Double, reason: String) -> Unit
) {
    var quantity by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("🗑️ Catat Wastage", color = TextPrimary) },
        text = {
            Column {
                Text(ingredient.name, color = AccentOrange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "Stok saat ini: ${"%.1f".format(ingredient.currentStock)} ${ingredient.defaultUnit.symbol}",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Jumlah terbuang (${ingredient.defaultUnit.symbol})", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentOrange,
                        unfocusedBorderColor = Color(0xFF2A2A4A)
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Alasan (tumpah / basi / kalibrasi)", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentOrange,
                        unfocusedBorderColor = Color(0xFF2A2A4A)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantity.toDoubleOrNull()
                    if (qty != null && qty > 0 && reason.isNotBlank()) {
                        onConfirm(qty, reason)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
            ) {
                Text("Catat Wastage")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = TextSecondary) }
        }
    )
}
