package com.dakota.pos.ui.shift

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dakota.pos.domain.model.shift.CashMovement
import com.dakota.pos.domain.model.shift.CashMovementType
import com.dakota.pos.domain.model.shift.CashierShift
import com.dakota.pos.domain.model.shift.ShiftStatus
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
fun ShiftScreen(
    viewModel: ShiftViewModel,
    currentUserId: String = "cashier-1",
    currentUserName: String = "Kasir"
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeShift by viewModel.activeShift.collectAsState()
    val allShifts by viewModel.allShifts.collectAsState()
    val movements by viewModel.movements.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Shift Aktif / Buka Shift, 1: Riwayat Shift

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
                        Text("💵 Shift & Cash Management", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Phase 10",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentGreen,
                            modifier = Modifier
                                .background(AccentGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCard)
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
                selectedTabIndex = activeTab,
                containerColor = DarkCard,
                contentColor = AccentBlue
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        val label = if (activeShift != null) "Shift Aktif (🟢 OPEN)" else "Buka Shift Kasir"
                        Text(label, color = if (activeTab == 0) AccentBlue else TextSecondary)
                    },
                    icon = { Icon(Icons.Default.Receipt, contentDescription = null, tint = if (activeTab == 0) AccentBlue else TextSecondary) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Riwayat Shift (${allShifts.size})", color = if (activeTab == 1) AccentBlue else TextSecondary) },
                    icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = if (activeTab == 1) AccentBlue else TextSecondary) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (activeTab == 0) {
                    if (activeShift == null) {
                        NoActiveShiftView(
                            onOpenShiftClick = { viewModel.openOpenShiftDialog() },
                            currentUserName = currentUserName
                        )
                    } else {
                        ActiveShiftDashboard(
                            shift = activeShift!!,
                            movements = movements,
                            onCashInClick = { viewModel.openCashMovementDialog(CashMovementType.CASH_IN) },
                            onExpenseClick = { viewModel.openCashMovementDialog(CashMovementType.EXPENSE) },
                            onXReportClick = { viewModel.generateXReport() },
                            onCloseShiftClick = { viewModel.openCloseShiftDialog() }
                        )
                    }
                } else {
                    ShiftHistoryView(
                        shifts = allShifts,
                        onViewReport = { viewModel.viewHistoricalZReport(it) }
                    )
                }
            }
        }
    }

    // Dialogs
    if (uiState.showOpenShiftDialog) {
        OpenShiftDialog(
            cashierName = currentUserName,
            onDismiss = { viewModel.closeOpenShiftDialog() },
            onConfirm = { openingCash ->
                viewModel.openShift(currentUserId, currentUserName, openingCash)
            }
        )
    }

    if (uiState.showCashMovementDialog) {
        CashMovementDialog(
            initialType = uiState.cashMovementType,
            onDismiss = { viewModel.closeCashMovementDialog() },
            onConfirm = { type, amount, reason ->
                viewModel.recordCashMovement(type, amount, reason, currentUserId, currentUserName)
            }
        )
    }

    if (uiState.showCloseShiftDialog && activeShift != null) {
        CloseShiftDialog(
            shift = activeShift!!,
            onDismiss = { viewModel.closeCloseShiftDialog() },
            onConfirmClose = { actualCash, notes ->
                viewModel.closeShift(actualCash, notes)
            }
        )
    }

    if (uiState.showReportPreviewDialog && uiState.reportPreviewText != null) {
        ReportPreviewDialog(
            title = uiState.reportPreviewTitle,
            reportText = uiState.reportPreviewText!!,
            onDismiss = { viewModel.closeReportPreviewDialog() }
        )
    }
}

// ============================================================================
// 1. NO ACTIVE SHIFT VIEW
// ============================================================================

@Composable
private fun NoActiveShiftView(
    onOpenShiftClick: () -> Unit,
    currentUserName: String
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.widthIn(max = 520.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AccentOrange.copy(alpha = 0.15f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Laci Kasir Belum Dibuka",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Halo $currentUserName! Untuk dapat memulai transaksi kasir dan mencatat kas laci secara akurat, harap masukkan modal awal kasir dan buka shift terlebih dahulu.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onOpenShiftClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buka Shift Kasir Sekarang", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================================================
// 2. ACTIVE SHIFT DASHBOARD
// ============================================================================

@Composable
private fun ActiveShiftDashboard(
    shift: CashierShift,
    movements: List<CashMovement>,
    onCashInClick: () -> Unit,
    onExpenseClick: () -> Unit,
    onXReportClick: () -> Unit,
    onCloseShiftClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AccentGreen.copy(alpha = 0.2f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = AccentGreen)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("SHIFT AKTIF", fontWeight = FontWeight.Bold, color = AccentGreen, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = AccentGreen,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("OPEN", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Text("Kasir: ${shift.cashierName} • Dibuka: ${formatDate(shift.startTime)}", color = TextSecondary, fontSize = 12.sp)
                        }
                    }

                    // Quick Action Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onXReportClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGold),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cetak X-Report")
                        }

                        Button(
                            onClick = onCloseShiftClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tutup Shift (Z-Report)")
                        }
                    }
                }
            }
        }

        // Metrics Grid (Cards)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShiftMetricCard(
                        title = "Modal Awal Laci",
                        amount = shift.openingCash,
                        subtitle = "Opening Cash",
                        accentColor = AccentBlue,
                        icon = Icons.Default.Lock,
                        modifier = Modifier.weight(1f)
                    )
                    ShiftMetricCard(
                        title = "Penjualan Tunai (+)",
                        amount = shift.cashSales,
                        subtitle = "${shift.totalTransactions} transaksi",
                        accentColor = AccentGreen,
                        icon = Icons.Default.Receipt,
                        modifier = Modifier.weight(1f)
                    )
                    ShiftMetricCard(
                        title = "Penjualan Non-Tunai",
                        amount = shift.nonCashSales,
                        subtitle = "QRIS / Debit / Card",
                        accentColor = AccentPurple,
                        icon = Icons.Default.Send,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShiftMetricCard(
                        title = "Kas Masuk (+)",
                        amount = shift.totalCashIn,
                        subtitle = "Setoran tambahan",
                        accentColor = AccentBlue,
                        icon = Icons.Default.ArrowDownward,
                        modifier = Modifier.weight(1f)
                    )
                    ShiftMetricCard(
                        title = "Kas Keluar / Biaya (-)",
                        amount = shift.totalCashOut + shift.totalExpense + shift.totalRefund,
                        subtitle = "Petty cash & refund",
                        accentColor = AccentRed,
                        icon = Icons.Default.ArrowUpward,
                        modifier = Modifier.weight(1f)
                    )
                    ShiftMetricCard(
                        title = "Estimasi Kas Laci (Expected)",
                        amount = shift.calculateExpectedCash(),
                        subtitle = "Uang fisik seharusnya",
                        accentColor = AccentGold,
                        icon = Icons.Default.Calculate,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Cash Movement Actions
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Catat Mutasi Kas Laci", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                        Text("Gunakan untuk mencatat setoran darurat atau belanja bahan/es batu kecil.", color = TextSecondary, fontSize = 12.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onCashInClick,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Kas Masuk (Cash In)")
                        }

                        Button(
                            onClick = onExpenseClick,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("- Petty Cash / Biaya")
                        }
                    }
                }
            }
        }

        // Shift Cash Movements History
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Riwayat Mutasi Kas Shift Ini (${movements.size})", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (movements.isEmpty()) {
                        Text("Belum ada mutasi kas masuk/keluar pada shift ini.", color = TextSecondary, fontSize = 13.sp)
                    } else {
                        movements.forEach { m ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkCardElevated, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val (badgeBg, badgeText) = when (m.type) {
                                        CashMovementType.CASH_IN -> AccentBlue.copy(alpha = 0.2f) to AccentBlue
                                        CashMovementType.EXPENSE -> AccentOrange.copy(alpha = 0.2f) to AccentOrange
                                        CashMovementType.CASH_OUT -> AccentRed.copy(alpha = 0.2f) to AccentRed
                                        CashMovementType.REFUND -> AccentPurple.copy(alpha = 0.2f) to AccentPurple
                                    }
                                    Surface(
                                        color = badgeBg,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(m.type.name, color = badgeText, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(m.reason, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Text(formatDate(m.timestamp), color = TextSecondary, fontSize = 11.sp)
                                    }
                                }

                                val sign = if (m.type == CashMovementType.CASH_IN) "+" else "-"
                                val color = if (m.type == CashMovementType.CASH_IN) AccentGreen else AccentRed
                                Text(
                                    "$sign${formatRupiah(m.amount)}",
                                    color = color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftMetricCard(
    title: String,
    amount: Double,
    subtitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formatRupiah(amount),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = accentColor, fontSize = 11.sp)
        }
    }
}

// ============================================================================
// 3. SHIFT HISTORY VIEW
// ============================================================================

@Composable
private fun ShiftHistoryView(
    shifts: List<CashierShift>,
    onViewReport: (CashierShift) -> Unit
) {
    if (shifts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada riwayat shift tersimpan.", color = TextSecondary)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(shifts) { shift ->
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Shift: ${shift.cashierName}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = if (shift.status == ShiftStatus.OPEN) AccentGreen.copy(alpha = 0.2f) else Color(0xFF455A64),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        shift.status.name,
                                        color = if (shift.status == ShiftStatus.OPEN) AccentGreen else Color(0xFFCFD8DC),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = { onViewReport(shift) },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkCardElevated),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentGold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Lihat Z-Report", fontSize = 12.sp, color = AccentGold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Waktu: ${formatDate(shift.startTime)} s/d ${shift.endTime?.let { formatDate(it) } ?: "Sekarang"}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = DarkSurface)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Modal Awal", color = TextSecondary, fontSize = 11.sp)
                                Text(formatRupiah(shift.openingCash), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Penjualan Tunai", color = TextSecondary, fontSize = 11.sp)
                                Text(formatRupiah(shift.cashSales), color = AccentGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Ekspektasi Kas", color = TextSecondary, fontSize = 11.sp)
                                Text(formatRupiah(shift.expectedCash), color = AccentGold, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Aktual Fisik", color = TextSecondary, fontSize = 11.sp)
                                Text(shift.actualCash?.let { formatRupiah(it) } ?: "-", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Selisih", color = TextSecondary, fontSize = 11.sp)
                                val v = shift.variance ?: 0.0
                                val vColor = when {
                                    v > 0 -> AccentGreen
                                    v < 0 -> AccentRed
                                    else -> TextSecondary
                                }
                                Text(
                                    text = if (shift.actualCash != null) formatRupiah(v) else "-",
                                    color = vColor,
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
// 4. DIALOGS: OPEN SHIFT, CASH MOVEMENT, CLOSE SHIFT, REPORT PREVIEW
// ============================================================================

@Composable
fun OpenShiftDialog(
    cashierName: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("200000") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkCard,
            modifier = Modifier.widthIn(max = 440.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Buka Shift Kasir Baru", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Kasir: $cashierName", color = AccentBlue, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Masukkan Modal Awal Laci (Opening Cash):", color = TextPrimary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Modal Awal (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Pilihan Cepat Nominal:", color = TextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(100000, 200000, 300000, 500000).forEach { nom ->
                        FilterChip(
                            selected = amountText == nom.toString(),
                            onClick = { amountText = nom.toString() },
                            label = { Text("Rp ${nom / 1000}k") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            if (amount >= 0) onConfirm(amount)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Buka Shift")
                    }
                }
            }
        }
    }
}

@Composable
fun CashMovementDialog(
    initialType: CashMovementType,
    onDismiss: () -> Unit,
    onConfirm: (CashMovementType, Double, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var amountText by remember { mutableStateOf("") }
    var reasonText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkCard,
            modifier = Modifier.widthIn(max = 460.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Catat Mutasi Kas Laci", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Jenis Mutasi:", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(CashMovementType.CASH_IN, CashMovementType.EXPENSE, CashMovementType.CASH_OUT).forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = {
                                val label = when (t) {
                                    CashMovementType.CASH_IN -> "+ Kas Masuk"
                                    CashMovementType.EXPENSE -> "- Petty Cash / Biaya"
                                    CashMovementType.CASH_OUT -> "- Kas Keluar"
                                    CashMovementType.REFUND -> "- Refund"
                                }
                                Text(label)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Nominal (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    label = { Text("Keterangan / Keperluan *") },
                    placeholder = { Text("misal: Beli es batu kristal 3 karung") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            if (amount > 0 && reasonText.isNotBlank()) {
                                onConfirm(selectedType, amount, reasonText)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Simpan Mutasi")
                    }
                }
            }
        }
    }
}

@Composable
fun CloseShiftDialog(
    shift: CashierShift,
    onDismiss: () -> Unit,
    onConfirmClose: (Double, String?) -> Unit
) {
    var actualCashText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    val actualCash = actualCashText.toDoubleOrNull()
    val expectedCash = shift.calculateExpectedCash()
    val variance = if (actualCash != null) actualCash - expectedCash else null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkCard,
            modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🔒 Tutup Shift & Rekonsiliasi Kasir", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                        Text("Kasir: ${shift.cashierName} • Dibuka: ${formatDate(shift.startTime)}", color = AccentBlue, fontSize = 13.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card info blind cash count
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Protokol Blind Cash Count:", fontWeight = FontWeight.Bold, color = AccentGold, fontSize = 13.sp)
                        Text(
                            "Hitung seluruh uang fisik di laci kasir (uang kertas dan koin). Masukkan jumlah total fisik aktual di bawah.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = actualCashText,
                    onValueChange = { actualCashText = it },
                    label = { Text("Total Uang Fisik Aktual di Laci (Rp) *") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentGold
                    )
                )

                // Feedback Rekonsiliasi realtime jika sudah diisi
                if (actualCash != null && variance != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                variance == 0.0 -> AccentGreen.copy(alpha = 0.15f)
                                variance > 0.0 -> AccentBlue.copy(alpha = 0.15f)
                                else -> AccentRed.copy(alpha = 0.15f)
                            }
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Ekspektasi Sistem:", color = TextSecondary, fontSize = 13.sp)
                                Text(formatRupiah(expectedCash), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Uang Fisik Dihitung:", color = TextSecondary, fontSize = 13.sp)
                                Text(formatRupiah(actualCash), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = DarkSurface)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val statusText = when {
                                    variance == 0.0 -> "HASIL: PAS (BALANCED) ✅"
                                    variance > 0.0 -> "HASIL: LEBIH (OVER) ⬆"
                                    else -> "HASIL: KURANG (SHORT) ⬇"
                                }
                                val statusColor = when {
                                    variance == 0.0 -> AccentGreen
                                    variance > 0.0 -> AccentBlue
                                    else -> AccentRed
                                }
                                Text(statusText, fontWeight = FontWeight.Bold, color = statusColor, fontSize = 14.sp)
                                Text(
                                    "${if (variance > 0) "+" else ""}${formatRupiah(variance)}",
                                    fontWeight = FontWeight.Black,
                                    color = statusColor,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Catatan Penutupan Shift / Keterangan Selisih") },
                    placeholder = { Text("Jelaskan alasan jika ada selisih kas...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (actualCash != null && actualCash >= 0) {
                                onConfirmClose(actualCash, notesText)
                            }
                        },
                        enabled = actualCash != null && actualCash >= 0,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Konfirmasi Tutup Shift & Cetak Z-Report")
                    }
                }
            }
        }
    }
}

@Composable
fun ReportPreviewDialog(
    title: String,
    reportText: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkCard,
            modifier = Modifier.fillMaxWidth(0.85f).fillMaxHeight(0.85f)
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
                    Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Monospace Struk Preview
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = reportText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFF00FF66), // Retro POS green terminal aesthetic
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                        Text("Tutup")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kirim ke Printer Struk")
                    }
                }
            }
        }
    }
}
