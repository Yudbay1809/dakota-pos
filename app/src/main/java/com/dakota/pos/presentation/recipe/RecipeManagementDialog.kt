package com.dakota.pos.presentation.recipe

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dakota.pos.domain.model.recipe.HppBreakdown
import com.dakota.pos.domain.model.recipe.Ingredient
import com.dakota.pos.domain.model.unit.UnitType
import com.dakota.pos.presentation.theme.CoffeeCaramel
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

@Composable
fun RecipeManagementDialog(
    viewModel: RecipeViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.92f),
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
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Calculate,
                                contentDescription = null,
                                tint = CoffeeCaramel,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Manajemen Resep & HPP Engine",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = PosTextPrimaryDark
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Kalkulasi otomatis HPP / COGS berbasis resep aktual dan harga bahan baku",
                            style = MaterialTheme.typography.bodySmall,
                            color = PosTextSecondaryDark
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = PosTextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = uiState.activeTab.ordinal,
                    containerColor = PosSurfaceElevatedDark,
                    contentColor = CoffeeCaramel,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[uiState.activeTab.ordinal]),
                            color = CoffeeCaramel
                        )
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, PosBorderDark, RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = uiState.activeTab == RecipeTab.HPP_CALCULATOR,
                        onClick = { viewModel.selectTab(RecipeTab.HPP_CALCULATOR) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Kalkulasi HPP & Resep Produk", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = uiState.activeTab == RecipeTab.INGREDIENTS_LIST,
                        onClick = { viewModel.selectTab(RecipeTab.INGREDIENTS_LIST) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Daftar Bahan Baku (${uiState.ingredients.size})", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status message banner
                uiState.statusMessage?.let { msg ->
                    Surface(
                        color = StatusSuccess.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(text = msg, color = StatusSuccess, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                    }
                }

                // Error message banner
                uiState.errorMessage?.let { err ->
                    Surface(
                        color = StatusError.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, StatusError.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(text = err, color = StatusError, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                    }
                }

                // Content Tab Views
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CoffeeCaramel)
                    }
                } else {
                    when (uiState.activeTab) {
                        RecipeTab.HPP_CALCULATOR -> {
                            HppCalculatorTabContent(
                                uiState = uiState,
                                onSelectProduct = { viewModel.selectProduct(it) },
                                onSelectVariant = { viewModel.selectVariant(it) }
                            )
                        }
                        RecipeTab.INGREDIENTS_LIST -> {
                            IngredientsTabContent(
                                ingredients = uiState.ingredients,
                                onAddClick = { viewModel.openAddIngredientDialog() },
                                onEditClick = { viewModel.openEditIngredientDialog(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Ingredient Edit / Add Dialog
    if (uiState.showIngredientDialog) {
        IngredientFormDialog(
            existingIngredient = uiState.editingIngredient,
            onSave = { name, sku, cat, unit, cost, minStock ->
                viewModel.saveIngredient(name, sku, cat, unit, cost, minStock)
            },
            onDismiss = { viewModel.dismissIngredientDialog() }
        )
    }
}

// ============================================================================
//  HPP CALCULATOR TAB CONTENT
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HppCalculatorTabContent(
    uiState: RecipeUiState,
    onSelectProduct: (com.dakota.pos.domain.model.Product) -> Unit,
    onSelectVariant: (com.dakota.pos.domain.model.ProductVariant) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Product Selector Bar
        Text(
            "PILIH PRODUK",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = CoffeeLatte
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(uiState.products) { product ->
                val isSelected = product.id == uiState.selectedProduct?.id
                Surface(
                    modifier = Modifier.clickable { onSelectProduct(product) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) CoffeeCaramel else PosSurfaceElevatedDark,
                    border = BorderStroke(1.dp, if (isSelected) CoffeeCaramel else PosBorderDark)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) CoffeeEspressoDark else PosTextPrimaryDark,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Variants if available
        val variants = uiState.selectedProduct?.variants ?: emptyList()
        if (variants.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Varian: ",
                    style = MaterialTheme.typography.labelSmall,
                    color = PosTextSecondaryDark
                )
                Spacer(modifier = Modifier.width(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    variants.forEach { variant ->
                        val isSelected = variant.id == uiState.selectedVariant?.id
                        Surface(
                            modifier = Modifier.clickable { onSelectVariant(variant) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) CoffeeWarmBrown.copy(alpha = 0.3f) else PosSurfaceDark,
                            border = BorderStroke(1.dp, if (isSelected) CoffeeCaramel else PosBorderDark)
                        ) {
                            Text(
                                text = variant.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) CoffeeCaramel else PosTextSecondaryDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // HPP Cards & Breakdown
        val breakdown = uiState.hppBreakdown
        if (breakdown != null && breakdown.itemBreakdowns.isNotEmpty()) {
            // KPI Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Harga Jual",
                    value = formatRupiah(breakdown.sellingPrice),
                    color = PosTextPrimaryDark,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Total HPP (COGS)",
                    value = formatRupiah(breakdown.totalHpp),
                    color = CoffeeCaramel,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Laba Kotor",
                    value = formatRupiah(breakdown.grossProfit),
                    color = StatusSuccess,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Gross Margin",
                    value = "%.1f%%".format(breakdown.grossMarginPercentage),
                    color = if (breakdown.grossMarginPercentage >= 50.0) StatusSuccess else Color(0xFFFFB300),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Detail Composition Table
            Text(
                "KOMPOSISI BAHAN BAKU RESEP",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = CoffeeLatte
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PosSurfaceElevatedDark,
                border = BorderStroke(1.dp, PosBorderDark),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                LazyColumn(modifier = Modifier.padding(14.dp)) {
                    item {
                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PosSurfaceDark, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bahan Baku", color = PosTextSecondaryDark, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.35f))
                            Text("Takaran", color = PosTextSecondaryDark, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.20f))
                            Text("Biaya Dasar", color = PosTextSecondaryDark, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.25f))
                            Text("Subtotal HPP", color = PosTextSecondaryDark, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.20f))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    items(breakdown.itemBreakdowns) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(0.35f)) {
                                Text(
                                    item.ingredientName,
                                    color = PosTextPrimaryDark,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Text(
                                "${item.recipeQuantity} ${item.recipeUnit.symbol}",
                                color = PosTextSecondaryDark,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(0.20f)
                            )
                            Text(
                                "${formatRupiah(item.ingredientBaseCost)} / ${item.ingredientBaseUnit.symbol}",
                                color = PosTextSecondaryDark,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(0.25f)
                            )
                            Text(
                                formatRupiah(item.subtotalHpp),
                                color = CoffeeCaramel,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.20f)
                            )
                        }
                        HorizontalDivider(color = PosBorderDark.copy(alpha = 0.5f))
                    }
                }
            }
        } else {
            // Empty State
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(PosSurfaceElevatedDark, RoundedCornerShape(12.dp))
                    .border(1.dp, PosBorderDark, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = PosTextMutedDark, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Belum ada resep yang dikonfigurasi untuk produk ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PosTextSecondaryDark
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = PosSurfaceElevatedDark,
        border = BorderStroke(1.dp, PosBorderDark)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = PosTextSecondaryDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = color)
        }
    }
}

// ============================================================================
//  INGREDIENTS LIST TAB CONTENT
// ============================================================================

@Composable
private fun IngredientsTabContent(
    ingredients: List<Ingredient>,
    onAddClick: () -> Unit,
    onEditClick: (Ingredient) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "KATALOG BAHAN BAKU MENTAH",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = CoffeeLatte
            )

            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoffeeCaramel),
                modifier = Modifier.height(38.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = CoffeeEspressoDark, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tambah Bahan Baku", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = CoffeeEspressoDark)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = PosSurfaceElevatedDark,
            border = BorderStroke(1.dp, PosBorderDark),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            LazyColumn(modifier = Modifier.padding(12.dp)) {
                items(ingredients) { ing ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditClick(ing) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    ing.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = PosTextPrimaryDark
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = PosSurfaceDark,
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, PosBorderDark)
                                ) {
                                    Text(
                                        ing.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CoffeeCaramel,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "SKU: ${ing.sku} • Min. Stok: ${ing.minimumStock} ${ing.defaultUnit.symbol}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PosTextMutedDark
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${formatRupiah(ing.costPerDefaultUnit)} / ${ing.defaultUnit.symbol}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = CoffeeCaramel
                                )
                                Text(
                                    "Stok: ${ing.currentStock} ${ing.defaultUnit.symbol}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PosTextSecondaryDark
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            IconButton(onClick = { onEditClick(ing) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PosTextSecondaryDark, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    HorizontalDivider(color = PosBorderDark.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// ============================================================================
//  INGREDIENT FORM DIALOG
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IngredientFormDialog(
    existingIngredient: Ingredient?,
    onSave: (String, String, String, UnitType, Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existingIngredient?.name ?: "") }
    var sku by remember { mutableStateOf(existingIngredient?.sku ?: "") }
    var category by remember { mutableStateOf(existingIngredient?.category ?: "Bahan Baku") }
    var selectedUnit by remember { mutableStateOf(existingIngredient?.defaultUnit ?: UnitType.G) }
    var costInput by remember { mutableStateOf(existingIngredient?.costPerDefaultUnit?.toLong()?.toString() ?: "") }
    var minStockInput by remember { mutableStateOf(existingIngredient?.minimumStock?.toInt()?.toString() ?: "0") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.55f),
            shape = RoundedCornerShape(18.dp),
            color = PosSurfaceDark,
            border = BorderStroke(1.dp, PosBorderDark)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = if (existingIngredient != null) "Edit Bahan Baku" else "Tambah Bahan Baku Baru",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = PosTextPrimaryDark
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Bahan Baku", color = PosTextMutedDark) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = inputColors()
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("Kode SKU", color = PosTextMutedDark) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = inputColors()
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Kategori", color = PosTextMutedDark) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = inputColors()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Text("Satuan Dasar Pengukuran:", style = MaterialTheme.typography.bodySmall, color = PosTextSecondaryDark)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UnitType.entries.forEach { unit ->
                        val isSelected = unit == selectedUnit
                        Surface(
                            modifier = Modifier.clickable { selectedUnit = unit },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) CoffeeCaramel else PosSurfaceElevatedDark,
                            border = BorderStroke(1.dp, if (isSelected) CoffeeCaramel else PosBorderDark)
                        ) {
                            Text(
                                "${unit.displayName} (${unit.symbol})",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) CoffeeEspressoDark else PosTextPrimaryDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = costInput,
                        onValueChange = { costInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Biaya / ${selectedUnit.symbol}", color = PosTextMutedDark) },
                        prefix = { Text("Rp ", color = CoffeeCaramel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = inputColors()
                    )

                    OutlinedTextField(
                        value = minStockInput,
                        onValueChange = { minStockInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Batas Min. Stok", color = PosTextMutedDark) },
                        suffix = { Text(selectedUnit.symbol, color = PosTextSecondaryDark) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = inputColors()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PosBorderDark),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PosTextSecondaryDark)
                    ) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val cost = costInput.toDoubleOrNull() ?: 0.0
                            val minStock = minStockInput.toDoubleOrNull() ?: 0.0
                            onSave(name, sku, category, selectedUnit, cost, minStock)
                        },
                        enabled = name.isNotBlank() && costInput.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoffeeCaramel)
                    ) {
                        Text("Simpan", color = CoffeeEspressoDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun inputColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = PosSurfaceElevatedDark,
    unfocusedContainerColor = PosSurfaceElevatedDark,
    focusedBorderColor = CoffeeCaramel,
    unfocusedBorderColor = PosBorderDark,
    cursorColor = CoffeeCaramel,
    focusedTextColor = PosTextPrimaryDark,
    unfocusedTextColor = PosTextPrimaryDark
)

private fun formatRupiah(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("in", "ID"))
    return "Rp " + formatter.format(amount.toLong())
}
