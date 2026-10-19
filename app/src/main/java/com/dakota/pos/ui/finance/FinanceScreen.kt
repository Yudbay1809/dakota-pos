package com.dakota.pos.ui.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dakota.pos.domain.model.finance.ExpenseCategory
import com.dakota.pos.domain.model.finance.OperatingExpense
import com.dakota.pos.domain.model.finance.ProductSalesMetric
import com.dakota.pos.domain.model.finance.ProfitLossReport
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// ─── Color Palette untuk Finansial ──────────────────────────────────────────
private val DarkBg = Color(0xFF121417)
private val DarkSurface = Color(0xFF1E2229)
private val DarkCard = Color(0xFF242933)
private val DarkBorder = Color(0xFF383E4C)

private val AccentGreen = Color(0xFF10B981)
private val AccentRed = Color(0xFFEF4444)
private val AccentAmber = Color(0xFFF59E0B)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentPurple = Color(0xFFA855F7)

private val TextPrimary = Color(0xFFF9FAFB)
private val TextSecondary = Color(0xFF9CA3AF)

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
fun FinanceScreen(
    viewModel: FinanceViewModel,
    currentUserName: String,
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val report = uiState.report

    LaunchedEffect(uiState.message, uiState.error) {
        if (uiState.message != null || uiState.error != null) {
            kotlinx.coroutines.delay(4000)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            Surface(
                color = DarkSurface,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AccentGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = "Finance",
                                    tint = AccentGreen,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Laporan Keuangan & Laba Rugi",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Analisis Penjualan • HPP Bahan Baku • Biaya Operasional • Profit",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Tombol Aksi Kanan
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { viewModel.openAddExpenseDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Catat Biaya Operasional", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            IconButton(
                                onClick = { viewModel.loadReport() },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DarkCard)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DarkCard)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Period Filter Chips & Navigation Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Filter Periode
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReportPeriod.entries.forEach { period ->
                                val isSelected = uiState.selectedPeriod == period
                                Surface(
                                    onClick = { viewModel.selectPeriod(period) },
                                    color = if (isSelected) AccentGreen.copy(alpha = 0.2f) else DarkCard,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) AccentGreen else DarkBorder
                                    )
                                ) {
                                    Text(
                                        text = period.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Navigation Tabs
                        TabRow(
                            selectedTabIndex = uiState.activeTab,
                            containerColor = Color.Transparent,
                            contentColor = AccentGreen,
                            divider = {},
                            modifier = Modifier.width(420.dp)
                        ) {
                            Tab(
                                selected = uiState.activeTab == 0,
                                onClick = { viewModel.setTab(0) },
                                text = { Text("Ringkasan Laba Rugi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                            )
                            Tab(
                                selected = uiState.activeTab == 1,
                                onClick = { viewModel.setTab(1) },
                                text = { Text("Biaya Operasional", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                            )
                            Tab(
                                selected = uiState.activeTab == 2,
                                onClick = { viewModel.setTab(2) },
                                text = { Text("Profit Produk", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && report == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentGreen)
                }
            } else if (report != null) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. KPI Cards Row
                    item {
                        KpiCardsRow(report = report)
                    }

                    // 2. Tab Contents
                    when (uiState.activeTab) {
                        0 -> {
                            item { IncomeStatementCard(report = report) }
                            item { OperationalOverviewCard(report = report) }
                        }
                        1 -> {
                            item { ExpenseBreakdownSection(report = report, onDeleteExpense = { viewModel.deleteExpense(it) }) }
                        }
                        2 -> {
                            item { ProductProfitabilityTable(products = report.topProducts) }
                        }
                    }
                }
            }

            // Toast / Snackbar
            uiState.message?.let { msg ->
                Surface(
                    color = AccentGreen,
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = msg, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }

            uiState.error?.let { err ->
                Surface(
                    color = AccentRed,
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = err, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    // Modal Catat Biaya Operasional
    if (uiState.showAddExpenseDialog) {
        AddExpenseDialog(
            currentUserName = currentUserName,
            onDismiss = { viewModel.closeAddExpenseDialog() },
            onSave = { cat, amt, desc, rec, notes ->
                viewModel.recordExpense(cat, amt, desc, rec, notes)
            }
        )
    }
}

// ─── 5 KPI Summary Cards ────────────────────────────────────────────────────
@Composable
private fun KpiCardsRow(report: ProfitLossReport) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Net Sales
        KpiCard(
            title = "Penjualan Bersih",
            amount = report.netSales,
            subtitle = "Kotor: ${formatRupiah(report.grossSales)}",
            accentColor = AccentAmber,
            modifier = Modifier.weight(1f)
        )

        // 2. COGS (HPP Terjual)
        KpiCard(
            title = "HPP Terjual (COGS)",
            amount = report.totalCogs,
            subtitle = "Bahan baku terpakai",
            accentColor = AccentBlue,
            modifier = Modifier.weight(1f)
        )

        // 3. Gross Profit
        KpiCard(
            title = "Laba Kotor",
            amount = report.grossProfit,
            subtitle = "Margin: ${"%.1f".format(report.grossProfitMargin)}%",
            accentColor = AccentPurple,
            modifier = Modifier.weight(1f)
        )

        // 4. Operating Expenses
        KpiCard(
            title = "Biaya Operasional",
            amount = report.totalOperatingExpenses,
            subtitle = "${report.operatingExpenses.size} transaksi biaya",
            accentColor = AccentRed,
            modifier = Modifier.weight(1f)
        )

        // 5. Net Profit
        val isProfit = report.netProfit >= 0.0
        KpiCard(
            title = "Laba Bersih Akhir",
            amount = report.netProfit,
            subtitle = "Margin: ${"%.1f".format(report.netProfitMargin)}%",
            accentColor = if (isProfit) AccentGreen else AccentRed,
            isHighlight = true,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun KpiCard(
    title: String,
    amount: Double,
    subtitle: String,
    accentColor: Color,
    isHighlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isHighlight) accentColor.copy(alpha = 0.15f) else DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isHighlight) accentColor else DarkBorder
        ),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formatRupiah(amount),
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isHighlight) accentColor else TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

// ─── Income Statement Breakdown Card ────────────────────────────────────────
@Composable
private fun IncomeStatementCard(report: ProfitLossReport) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Laporan Laba Rugi Komprehensif (Income Statement)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Standar Akuntansi POS",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Income Statement Lines
            ReportLine(label = "Penjualan Kotor (Gross Sales)", value = report.grossSales, isHeader = true)
            ReportLine(label = "Potongan / Diskon Penjualan", value = -report.totalDiscount, isNegative = true)
            ReportLine(label = "Pengembalian Dana (Refund)", value = -report.totalRefund, isNegative = true)
            HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 8.dp))

            ReportLine(
                label = "Penjualan Bersih (Net Sales)",
                value = report.netSales,
                isSubtotal = true,
                accentColor = AccentAmber
            )

            Spacer(modifier = Modifier.height(6.dp))
            ReportLine(
                label = "Harga Pokok Penjualan (HPP / COGS)",
                value = -report.totalCogs,
                isNegative = true
            )
            HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 8.dp))

            ReportLine(
                label = "Laba Kotor (Gross Profit)",
                value = report.grossProfit,
                isSubtotal = true,
                percent = report.grossProfitMargin,
                accentColor = AccentPurple
            )

            Spacer(modifier = Modifier.height(6.dp))
            ReportLine(
                label = "Total Biaya Operasional (Operating Expenses)",
                value = -report.totalOperatingExpenses,
                isNegative = true
            )
            HorizontalDivider(color = DarkBorder, thickness = 2.dp, modifier = Modifier.padding(vertical = 10.dp))

            ReportLine(
                label = "LABA BERSIH (NET PROFIT)",
                value = report.netProfit,
                isTotal = true,
                percent = report.netProfitMargin,
                accentColor = if (report.netProfit >= 0) AccentGreen else AccentRed
            )
        }
    }
}

@Composable
private fun ReportLine(
    label: String,
    value: Double,
    isHeader: Boolean = false,
    isSubtotal: Boolean = false,
    isTotal: Boolean = false,
    isNegative: Boolean = false,
    percent: Double? = null,
    accentColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isTotal) 6.dp else 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = if (isTotal) 15.sp else 13.sp,
            fontWeight = when {
                isTotal -> FontWeight.Black
                isSubtotal || isHeader -> FontWeight.Bold
                else -> FontWeight.Normal
            },
            color = if (isTotal) accentColor else if (isSubtotal) TextPrimary else TextSecondary
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            percent?.let { p ->
                Text(
                    text = "(${ "%.1f".format(p) }%)",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
            Text(
                text = (if (isNegative && value < 0) "- " else "") + formatRupiah(kotlin.math.abs(value)),
                fontSize = if (isTotal) 17.sp else 13.sp,
                fontWeight = if (isTotal || isSubtotal) FontWeight.Bold else FontWeight.Normal,
                color = if (isTotal || isSubtotal) accentColor else if (isNegative) AccentRed else TextPrimary
            )
        }
    }
}

// ─── Operational Metrics Card ───────────────────────────────────────────────
@Composable
private fun OperationalOverviewCard(report: ProfitLossReport) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Metrik Operasional & Volume Transaksi",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetricItem(label = "Total Transaksi Pesanan", value = "${report.totalOrdersCount} Order")
                MetricItem(label = "Total Item Terjual", value = "${report.totalItemsSold} Porsi / Cup")
                MetricItem(label = "Rata-rata Nilai Transaksi (AOV)", value = formatRupiah(report.averageOrderValue))
            }
        }
    }
}

@Composable
private fun RowScope.MetricItem(label: String, value: String) {
    Surface(
        color = DarkCard,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.weight(1f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = label, fontSize = 11.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

// ─── Operating Expense Breakdown Table ──────────────────────────────────────
@Composable
private fun ExpenseBreakdownSection(
    report: ProfitLossReport,
    onDeleteExpense: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Rincian Biaya Operasional Toko",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Breakdown Per Category Summary
            if (report.expenseBreakdown.isNotEmpty()) {
                Text(text = "Ringkasan Kategori", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    report.expenseBreakdown.forEach { (cat, total) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = cat.displayName, fontSize = 13.sp, color = TextPrimary)
                            Text(text = formatRupiah(total), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AccentRed)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DarkBorder)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // List of Recent Operating Expenses
            Text(text = "Daftar Pengeluaran", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Spacer(modifier = Modifier.height(10.dp))

            if (report.operatingExpenses.isEmpty()) {
                Text(
                    text = "Belum ada biaya operasional yang dicatat pada periode ini.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    report.operatingExpenses.forEach { exp ->
                        Surface(
                            color = DarkCard,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = exp.description,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(AccentRed.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = exp.category.displayName,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentRed
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${formatDate(exp.date)} • Oleh: ${exp.recordedBy}" + if (!exp.notes.isNullOrBlank()) " (${exp.notes})" else "",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = formatRupiah(exp.amount),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentRed
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { onDeleteExpense(exp.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Menu Item Profitability Table ──────────────────────────────────────────
@Composable
private fun ProductProfitabilityTable(products: List<ProductSalesMetric>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Analisis Profitabilitas Menu Produk",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (products.isEmpty()) {
                Text(
                    text = "Belum ada transaksi menu pada periode ini.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Menu Produk", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(2f))
                    Text(text = "Qty", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(0.7f))
                    Text(text = "Omzet", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
                    Text(text = "COGS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
                    Text(text = "Laba Kotor", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
                    Text(text = "Margin", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                }
                HorizontalDivider(color = DarkBorder)

                Column(modifier = Modifier.fillMaxWidth()) {
                    products.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = item.productName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(2f))
                            Text(text = "${item.quantitySold}x", fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(0.7f))
                            Text(text = formatRupiah(item.revenue), fontSize = 13.sp, color = TextPrimary, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
                            Text(text = formatRupiah(item.cogs), fontSize = 13.sp, color = AccentBlue, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
                            Text(text = formatRupiah(item.grossProfit), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentPurple, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
                            Text(text = "${"%.1f".format(item.marginPercent)}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentGreen, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        }
                        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

// ─── Modal Catat Biaya Operasional ──────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseDialog(
    currentUserName: String,
    onDismiss: () -> Unit,
    onSave: (ExpenseCategory, Double, String, String, String?) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.ELECTRICITY) }
    var amountInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier
                .widthIn(min = 440.dp, max = 520.dp)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Catat Biaya Operasional",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Kategori Selector
                Text(text = "Kategori Pengeluaran", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))

                // Grid Kategori Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val chunks = ExpenseCategory.entries.chunked(3)
                    chunks.forEach { rowCategories ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowCategories.forEach { cat ->
                                val isSelected = selectedCategory == cat
                                Surface(
                                    onClick = { selectedCategory = cat },
                                    color = if (isSelected) AccentRed.copy(alpha = 0.2f) else DarkCard,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) AccentRed else DarkBorder
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cat.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) TextPrimary else TextSecondary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Nominal Biaya
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { char -> char.isDigit() } },
                    label = { Text("Nominal Biaya (Rp)", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentRed,
                        unfocusedBorderColor = DarkBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Keterangan / Deskripsi
                OutlinedTextField(
                    value = descriptionInput,
                    onValueChange = { descriptionInput = it },
                    label = { Text("Keterangan Biaya (cth: Token Listrik 100rb)", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentRed,
                        unfocusedBorderColor = DarkBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Catatan Tambahan
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Catatan / No. Struk (Opsional)", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentRed,
                        unfocusedBorderColor = DarkBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountInput.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0 && descriptionInput.isNotBlank()) {
                                onSave(selectedCategory, amt, descriptionInput, currentUserName, notesInput)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Simpan Pengeluaran", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
