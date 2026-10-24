package com.dakota.pos.ui.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dakota.pos.domain.auth.UserRole
import com.dakota.pos.domain.model.staff.*
import com.dakota.pos.presentation.theme.*
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
    maximumFractionDigits = 0
}
private fun formatRupiah(amount: Double): String = rupiahFormat.format(amount)

private fun formatTimestamp(timestamp: Long): String {
    val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
}

@Composable
fun StaffAttendancePayrollScreen(
    viewModel: StaffViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf("Presensi & Mesin Biometrik", "Slip Gaji Staf (Payroll)")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PosBackgroundDark)
            .padding(20.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Staff Attendance & Biometric Payroll",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Otomasi slip gaji berbasis data scan sidik jari terpisah dari operasional kasir",
                    color = PosTextSecondaryDark,
                    fontSize = 13.sp
                )
            }

            // Sync Status Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.12f))
                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Buffer Mesin Absensi: Aktif (Asinkron)",
                    color = Color(0xFF10B981),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notification Banner
        if (uiState.notificationMessage != null) {
            Surface(
                color = CoffeeCaramel.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeCaramel.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = uiState.notificationMessage!!,
                        color = CoffeeCream,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(
                        onClick = { viewModel.clearNotification() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = CoffeeCream)
                    }
                }
            }
        }

        // Navigation Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = PosSurfaceDark,
            contentColor = CoffeeCaramel,
            modifier = Modifier.clip(RoundedCornerShape(10.dp))
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) CoffeeCaramel else PosTextSecondaryDark
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            if (selectedTab == 0) {
                AttendanceTabContent(
                    uiState = uiState,
                    onSyncClick = { viewModel.syncFromBiometricDevice() },
                    onQuickScanClick = { viewModel.openScanDialog() },
                    onAdjustmentClick = { viewModel.openAdjustmentDialog() }
                )
            } else {
                PayrollTabContent(
                    uiState = uiState,
                    onSelectStaff = { viewModel.selectStaff(it) },
                    onPreviewSlip = { viewModel.generateAndShowSlipPreview() },
                    onSavePayroll = { viewModel.savePayrollToDatabaseAndFinance() }
                )
            }
        }
    }

    // Modal: Quick Scan Simulator
    if (uiState.showScanDialog) {
        QuickScanDialog(
            salaryRates = uiState.salaryRates,
            onDismiss = { viewModel.dismissScanDialog() },
            onConfirm = { userId, type ->
                viewModel.recordQuickScan(userId, type)
            }
        )
    }

    // Modal: Manual Adjustment
    if (uiState.showAdjustmentDialog) {
        ManualAdjustmentDialog(
            salaryRates = uiState.salaryRates,
            onDismiss = { viewModel.dismissAdjustmentDialog() },
            onConfirm = { userId, type, notes ->
                viewModel.recordQuickScan(userId, type, AttendanceSource.MANAGER_OVERRIDE, notes)
            }
        )
    }

    // Modal: Pay Slip Thermal Preview
    if (uiState.showReceiptDialog && uiState.receiptPreviewText != null) {
        PaySlipReceiptDialog(
            receiptText = uiState.receiptPreviewText!!,
            onPrintClick = { viewModel.printSlipToThermalPrinter() },
            onDismiss = { viewModel.dismissReceiptDialog() }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 1: ATTENDANCE & BIOMETRIC DEVICE BUFFER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AttendanceTabContent(
    uiState: StaffUiState,
    onSyncClick: () -> Unit,
    onQuickScanClick: () -> Unit,
    onAdjustmentClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Action Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Log Presensi Sidik Jari Terkumpul (${uiState.attendances.size} Data)",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Button 1: Sync Buffer
                Button(
                    onClick = onSyncClick,
                    enabled = !uiState.isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeEspressoDark),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CoffeeCaramel, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = CoffeeCream)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Tarik Log Buffer Mesin", color = CoffeeCream, fontSize = 12.sp)
                }

                // Button 2: Quick Scan
                Button(
                    onClick = onQuickScanClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeCaramel),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simulasi Scan Jari", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Button 3: Manual Adjustment
                OutlinedButton(
                    onClick = onAdjustmentClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PosTextSecondaryDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosBorderDark),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = PosTextSecondaryDark)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Koreksi Manual Manager", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Table List
        if (uiState.attendances.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PosSurfaceDark),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = PosTextSecondaryDark, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Belum ada log presensi sidik jari.", color = PosTextSecondaryDark, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Klik 'Tarik Log Buffer Mesin' atau 'Simulasi Scan Jari' untuk menguji.", color = CoffeeCaramel, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PosSurfaceDark)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.attendances) { log ->
                    AttendanceLogRow(log)
                }
            }
        }
    }
}

@Composable
private fun AttendanceLogRow(log: StaffAttendance) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PosSurfaceElevatedDark)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            // Type Badge IN vs OUT
            val isClockIn = log.type == AttendanceType.IN
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isClockIn) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isClockIn) "CLOCK-IN" else "CLOCK-OUT",
                    color = if (isClockIn) Color(0xFF10B981) else Color(0xFFF59E0B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.staffName,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "[${log.role.name}]",
                        color = PosTextSecondaryDark,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "${formatTimestamp(log.timestamp)} • Sumber: ${log.source.name}",
                    color = PosTextSecondaryDark,
                    fontSize = 12.sp
                )
            }
        }

        // Status Badge
        val (statusText, statusColor) = when (log.status) {
            AttendanceStatus.ON_TIME -> "TEPAT WAKTU" to Color(0xFF10B981)
            AttendanceStatus.LATE -> "TERLAMBAT (${log.lateMinutes}m)" to Color(0xFFEF4444)
            AttendanceStatus.OVERTIME -> "LEMBUR (+${log.overtimeHours}j)" to Color(0xFFF59E0B)
            AttendanceStatus.MANUAL_ADJUSTMENT -> "DISETUJUI MANUAL" to Color(0xFFA855F7)
            AttendanceStatus.NORMAL -> "NORMAL" to PosTextSecondaryDark
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(statusColor.copy(alpha = 0.15f))
                .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = statusText,
                color = statusColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 2: AUTOMATED PAYROLL & PAY SLIP CALCULATION
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PayrollTabContent(
    uiState: StaffUiState,
    onSelectStaff: (String) -> Unit,
    onPreviewSlip: () -> Unit,
    onSavePayroll: () -> Unit
) {
    val payroll = uiState.currentPayroll

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // Left Column: Staff Selector & Audit Summary (40% width)
        Column(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(PosSurfaceDark)
                .padding(14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Pilih Karyawan Kafe",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Staff Selection List
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                uiState.salaryRates.forEach { staff ->
                    val isSelected = staff.userId == uiState.selectedUserId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CoffeeCaramel.copy(alpha = 0.2f) else PosSurfaceElevatedDark)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) CoffeeCaramel else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectStaff(staff.userId) }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(staff.staffName, color = if (isSelected) CoffeeCream else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(staff.role.name, color = PosTextSecondaryDark, fontSize = 11.sp)
                        }
                        Text(formatRupiah(staff.baseSalary), color = CoffeeCaramel, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = PosBorderDark)
            Spacer(modifier = Modifier.height(12.dp))

            // Biometric Audit Mini-Dashboard
            Text(
                text = "Audit Absensi Terverifikasi",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (payroll != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AuditMetricRow("Hari Kehadiran Fisik", "${payroll.presentDays} / ${payroll.standardDays} Hari", Color(0xFF10B981))
                    AuditMetricRow("Lembur Terverifikasi", "${payroll.overtimeHours} Jam (+${formatRupiah(payroll.totalOvertimePay)})", Color(0xFFF59E0B))
                    AuditMetricRow("Keterlambatan Scan IN", "${payroll.lateCount} Kali (-${formatRupiah(payroll.totalLatePenalty)})", Color(0xFFEF4444))
                    AuditMetricRow("Uang Makan & Transport", formatRupiah(payroll.totalMealTransport), CoffeeCream)
                }
            }
        }

        // Right Column: Full Slip Calculation & Actions (58% width)
        Column(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(PosSurfaceDark)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Rincian Slip Gaji Periode ${uiState.periodMonth}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Staf: ${payroll?.staffName ?: "-"} (${payroll?.role?.name ?: "-"})",
                            color = CoffeeCaramel,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF383E4C))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("No: ${payroll?.id ?: "SAL-..."}", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Breakdown Slip Table
                if (payroll != null) {
                    SalaryLineItem("Gaji Pokok / Base Salary", formatRupiah(payroll.baseSalary), false)
                    SalaryLineItem("Tunjangan Makan & Transport (@Rp 35k)", "+${formatRupiah(payroll.totalMealTransport)}", false, Color(0xFF10B981))
                    SalaryLineItem("Upah Lembur Terverifikasi (@Rp 25k/jam)", "+${formatRupiah(payroll.totalOvertimePay)}", false, Color(0xFF10B981))
                    SalaryLineItem("Denda Keterlambatan Scan Jari (@Rp 15k)", "-${formatRupiah(payroll.totalLatePenalty)}", false, Color(0xFFEF4444))

                    if (payroll.otherDeductions > 0) {
                        SalaryLineItem("Potongan Kasbon / Lainnya", "-${formatRupiah(payroll.otherDeductions)}", false, Color(0xFFEF4444))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = PosBorderDark, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Net Salary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("TOTAL GAJI BERSIH (NET SALARY)", color = PosTextSecondaryDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Take Home Pay Karyawan", color = PosTextSecondaryDark, fontSize = 11.sp)
                        }
                        Text(
                            text = formatRupiah(payroll.netSalary),
                            color = CoffeeCaramel,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onSavePayroll,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoffeeCream),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoffeeCaramel)
                ) {
                    Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Posting ke Biaya Operasional (P&L)", fontSize = 12.sp)
                }

                Button(
                    onClick = onPreviewSlip,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeCaramel)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cetak Slip Gaji (Printer POS)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AuditMetricRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = PosTextSecondaryDark, fontSize = 12.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun SalaryLineItem(
    title: String,
    amount: String,
    isBold: Boolean,
    color: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = if (isBold) Color.White else PosTextSecondaryDark, fontSize = 13.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(amount, color = color, fontSize = 13.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIALOGS: SCAN SIMULATOR, MANUAL ADJUSTMENT & THERMAL RECEIPT PREVIEW
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun QuickScanDialog(
    salaryRates: List<StaffSalaryRate>,
    onDismiss: () -> Unit,
    onConfirm: (userId: String, type: AttendanceType) -> Unit
) {
    var selectedUserId by remember { mutableStateOf(salaryRates.firstOrNull()?.userId ?: "usr-barista-01") }
    var selectedType by remember { mutableStateOf(AttendanceType.IN) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E2229),
            modifier = Modifier.width(460.dp).padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Simulasi Pemindaian Sidik Jari", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Sentuhkan jari pada sensor biometrik", color = PosTextSecondaryDark, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(10.dp))

                // Staff Picker
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Pilih Karyawan:", color = PosTextSecondaryDark, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    salaryRates.forEach { staff ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedUserId == staff.userId) PosSurfaceDark else Color.Transparent)
                                .clickable { selectedUserId = staff.userId }
                                .padding(vertical = 4.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedUserId == staff.userId,
                                onClick = { selectedUserId = staff.userId },
                                colors = RadioButtonDefaults.colors(selectedColor = CoffeeCaramel)
                            )
                            Text("${staff.staffName} (${staff.role.name})", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Type IN vs OUT
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { selectedType = AttendanceType.IN },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == AttendanceType.IN) Color(0xFF10B981) else Color(0xFF2A2E39)
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clock-IN (Masuk)", fontSize = 12.sp, color = Color.White)
                    }

                    Button(
                        onClick = { selectedType = AttendanceType.OUT },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == AttendanceType.OUT) Color(0xFFF59E0B) else Color(0xFF2A2E39)
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clock-OUT (Pulang)", fontSize = 12.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Batal", color = PosTextSecondaryDark)
                    }
                    Button(
                        onClick = { onConfirm(selectedUserId, selectedType) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CoffeeCaramel)
                    ) {
                        Text("Scan Jari Sekarang", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualAdjustmentDialog(
    salaryRates: List<StaffSalaryRate>,
    onDismiss: () -> Unit,
    onConfirm: (userId: String, type: AttendanceType, notes: String) -> Unit
) {
    var selectedUserId by remember { mutableStateOf(salaryRates.firstOrNull()?.userId ?: "usr-barista-01") }
    var selectedType by remember { mutableStateOf(AttendanceType.IN) }
    var notesInput by remember { mutableStateOf("Lupa scan sidik jari saat datang") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E2229),
            modifier = Modifier.width(440.dp).padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                Text("Koreksi Presensi Manual oleh Manager", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Digunakan jika staf lupa scan atau mesin absen offline", color = PosTextSecondaryDark, fontSize = 11.sp)

                Spacer(modifier = Modifier.height(10.dp))

                Text("Pilih Karyawan:", color = PosTextSecondaryDark, fontSize = 12.sp)
                salaryRates.forEach { staff ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedUserId = staff.userId }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedUserId == staff.userId,
                            onClick = { selectedUserId = staff.userId },
                            colors = RadioButtonDefaults.colors(selectedColor = CoffeeCaramel)
                        )
                        Text("${staff.staffName} (${staff.role.name})", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Tipe Kehadiran:", color = PosTextSecondaryDark, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Button(
                        onClick = { selectedType = AttendanceType.IN },
                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == AttendanceType.IN) Color(0xFF10B981) else Color(0xFF2A2E39)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Masuk (IN)", fontSize = 11.sp, color = Color.White)
                    }
                    Button(
                        onClick = { selectedType = AttendanceType.OUT },
                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == AttendanceType.OUT) Color(0xFFF59E0B) else Color(0xFF2A2E39)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Pulang (OUT)", fontSize = 11.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Catatan Verifikasi Manager:", color = PosTextSecondaryDark, fontSize = 12.sp)
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CoffeeCaramel,
                        unfocusedBorderColor = PosBorderDark
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Batal", color = PosTextSecondaryDark)
                    }
                    Button(
                        onClick = { onConfirm(selectedUserId, selectedType, notesInput) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
                    ) {
                        Text("Setujui Koreksi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaySlipReceiptDialog(
    receiptText: String,
    onPrintClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E2229),
            modifier = Modifier.width(420.dp).padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pratinjau Slip Printer Thermal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Paper simulation
                Surface(
                    color = Color(0xFFFFFBEA),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .padding(horizontal = 8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = receiptText,
                        color = Color.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Tutup", color = PosTextSecondaryDark)
                    }
                    Button(
                        onClick = onPrintClick,
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(containerColor = CoffeeCaramel)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kirim ke Printer POS", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
