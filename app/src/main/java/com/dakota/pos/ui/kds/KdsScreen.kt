package com.dakota.pos.ui.kds

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dakota.pos.core.hardware.printer.PrinterType
import com.dakota.pos.domain.model.kds.KitchenOrder
import com.dakota.pos.domain.model.kds.KitchenOrderStatus
import com.dakota.pos.domain.model.kds.StationConfig
import com.dakota.pos.domain.model.kds.WorkStation
import java.text.SimpleDateFormat
import java.util.*

// ─── Color Palette untuk KDS High-Contrast ──────────────────────────────────
private val KdsBackground = Color(0xFF121417)
private val KdsSurface = Color(0xFF1E2229)
private val KdsSurfaceElevated = Color(0xFF282D37)
private val KdsCardBorder = Color(0xFF383E4C)

private val NormalGreen = Color(0xFF10B981)
private val WarningOrange = Color(0xFFF59E0B)
private val CriticalRed = Color(0xFFEF4444)
private val BaristaAmber = Color(0xFFD97706)
private val KitchenCyan = Color(0xFF06B6D4)
private val DessertPurple = Color(0xFFA855F7)

private val TextPrimary = Color(0xFFF9FAFB)
private val TextSecondary = Color(0xFF9CA3AF)

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale("id", "ID"))
    return sdf.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KdsScreen(
    viewModel: KdsViewModel,
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeOrders by viewModel.activeOrders.collectAsState()
    val stationConfigs by viewModel.stationConfigs.collectAsState()

    var showPrinterDialog by remember { mutableStateOf(false) }

    // SnackBar / Message Handler
    LaunchedEffect(uiState.message, uiState.error) {
        // Auto clear after 4 seconds
        if (uiState.message != null || uiState.error != null) {
            kotlinx.coroutines.delay(4000)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = KdsBackground,
        topBar = {
            Surface(
                color = KdsSurface,
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
                                    .background(BaristaAmber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = "KDS",
                                    tint = BaristaAmber,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Kitchen Display System (KDS)",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Antrean Barista & Dapur • Realtime Status Tracking",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Badge Pesanan Aktif
                            Surface(
                                color = if (activeOrders.isNotEmpty()) NormalGreen.copy(alpha = 0.2f) else KdsSurfaceElevated,
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (activeOrders.isNotEmpty()) NormalGreen else KdsCardBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (activeOrders.isNotEmpty()) NormalGreen else TextSecondary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${activeOrders.size} Pesanan Aktif",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (activeOrders.isNotEmpty()) NormalGreen else TextSecondary
                                    )
                                }
                            }
                        }

                        // Tombol Aksi Kanan
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { showPrinterDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, KdsCardBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Setting Printer", fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(KdsSurfaceElevated)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup KDS",
                                    tint = TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Station Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StationFilterChip(
                            label = "Semua Stasiun",
                            isSelected = uiState.selectedStation == null,
                            count = activeOrders.size,
                            onClick = { viewModel.selectStation(null) }
                        )

                        WorkStation.entries.forEach { station ->
                            val count = activeOrders.count { it.station == station }
                            val color = when (station) {
                                WorkStation.BAR -> BaristaAmber
                                WorkStation.KITCHEN -> KitchenCyan
                                WorkStation.DESSERT -> DessertPurple
                            }
                            StationFilterChip(
                                label = station.displayName,
                                isSelected = uiState.selectedStation == station,
                                count = count,
                                accentColor = color,
                                onClick = { viewModel.selectStation(station) }
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
            if (activeOrders.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(KdsSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = NormalGreen,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Semua Pesanan Selesai!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tidak ada antrean pesanan aktif saat ini. Pesanan baru dari kasir POS akan muncul otomatis di sini.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 440.dp)
                    )
                }
            } else {
                // Order Tickets Grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 340.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(activeOrders, key = { it.id }) { order ->
                        KitchenOrderCard(
                            order = order,
                            onAdvanceStatus = { viewModel.advanceOrderStatus(order.id) },
                            onCancelOrder = { viewModel.updateOrderStatus(order.id, KitchenOrderStatus.CANCELLED) },
                            onReprintTicket = { viewModel.reprintTicket(order) }
                        )
                    }
                }
            }

            // Toast / Banner Pesan
            uiState.message?.let { msg ->
                Surface(
                    color = NormalGreen,
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
                    color = CriticalRed,
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

    // Dialog Konfigurasi Printer
    if (showPrinterDialog) {
        PrinterConfigDialog(
            configs = stationConfigs,
            onDismiss = { showPrinterDialog = false },
            onSave = { config ->
                viewModel.saveConfig(config)
            }
        )
    }
}

// ─── Filter Chip Component ──────────────────────────────────────────────────
@Composable
private fun StationFilterChip(
    label: String,
    isSelected: Boolean,
    count: Int,
    accentColor: Color = NormalGreen,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) accentColor.copy(alpha = 0.2f) else KdsSurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) accentColor else KdsCardBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) TextPrimary else TextSecondary
            )
            if (count > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) accentColor else KdsCardBorder)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = count.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ─── Kitchen Order Ticket Card ──────────────────────────────────────────────
@Composable
private fun KitchenOrderCard(
    order: KitchenOrder,
    onAdvanceStatus: () -> Unit,
    onCancelOrder: () -> Unit,
    onReprintTicket: () -> Unit
) {
    // Timer Urgency Color
    val timerColor = when {
        order.isCritical -> CriticalRed
        order.isWarning -> WarningOrange
        else -> NormalGreen
    }

    val stationColor = when (order.station) {
        WorkStation.BAR -> BaristaAmber
        WorkStation.KITCHEN -> KitchenCyan
        WorkStation.DESSERT -> DessertPurple
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = KdsSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (order.isCritical) CriticalRed.copy(alpha = 0.8f) else KdsCardBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Order Number, Station, & Elapsed Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${order.orderNumber}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(stationColor.copy(alpha = 0.2f))
                            .border(1.dp, stationColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = order.station.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = stationColor
                        )
                    }
                }

                // Timer Badge
                Surface(
                    color = timerColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, timerColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (order.isCritical) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = CriticalRed,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = "${order.elapsedMinutes}m lalu",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = timerColor
                        )
                    }
                }
            }

            // Info Jam & Kasir
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Jam: ${formatTime(order.orderTime)}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = "Kasir: ${order.cashierName}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = KdsCardBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Order Items List
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                order.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Qty Badge
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(KdsSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${item.quantity}x",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BaristaAmber
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Product Name, Variant, Notes
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.productName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (!item.variantName.isNullOrBlank()) {
                                Text(
                                    text = "Varian: ${item.variantName}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            if (!item.modifiersSummary.isNullOrBlank()) {
                                Text(
                                    text = "+ ${item.modifiersSummary}",
                                    fontSize = 12.sp,
                                    color = BaristaAmber.copy(alpha = 0.9f)
                                )
                            }
                            if (!item.notes.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CriticalRed.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Catatan: ${item.notes}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CriticalRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Catatan Keseluruhan Order
            if (!order.customerNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = WarningOrange.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarningOrange.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Catatan Order: ${order.customerNotes}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = WarningOrange,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = KdsCardBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Status Saat Ini & Tombol Aksi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Pill
                val statusColor = when (order.status) {
                    KitchenOrderStatus.NEW -> CriticalRed
                    KitchenOrderStatus.ACCEPTED -> WarningOrange
                    KitchenOrderStatus.PREPARING -> BaristaAmber
                    KitchenOrderStatus.READY -> NormalGreen
                    KitchenOrderStatus.SERVED -> TextSecondary
                    KitchenOrderStatus.CANCELLED -> TextSecondary
                }

                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
                ) {
                    Text(
                        text = order.status.displayName.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Tombol Reprint
                IconButton(
                    onClick = onReprintTicket,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Cetak Tiket",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Action Button (State progression)
            val buttonText = when (order.status) {
                KitchenOrderStatus.NEW -> "Mulai Kerjakan (Diproses)"
                KitchenOrderStatus.ACCEPTED, KitchenOrderStatus.PREPARING -> "Tandai Siap Saji (Ready)"
                KitchenOrderStatus.READY -> "Sajikan ke Tamu (Served)"
                else -> "Selesai"
            }

            val buttonColor = when (order.status) {
                KitchenOrderStatus.NEW -> BaristaAmber
                KitchenOrderStatus.ACCEPTED, KitchenOrderStatus.PREPARING -> KitchenCyan
                KitchenOrderStatus.READY -> NormalGreen
                else -> KdsSurfaceElevated
            }

            Button(
                onClick = onAdvanceStatus,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(
                    imageVector = when (order.status) {
                        KitchenOrderStatus.NEW -> Icons.Default.PlayArrow
                        KitchenOrderStatus.ACCEPTED, KitchenOrderStatus.PREPARING -> Icons.Default.Check
                        else -> Icons.Default.Check
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buttonText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ─── Dialog Konfigurasi Printer Stasiun ──────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrinterConfigDialog(
    configs: List<StationConfig>,
    onDismiss: () -> Unit,
    onSave: (StationConfig) -> Unit
) {
    var selectedStation by remember { mutableStateOf(WorkStation.BAR) }
    val currentConfig = configs.find { it.station == selectedStation }
        ?: StationConfig(station = selectedStation, name = selectedStation.displayName)

    var printerType by remember(currentConfig) { mutableStateOf(currentConfig.printerType) }
    var targetAddress by remember(currentConfig) { mutableStateOf(currentConfig.targetAddress) }
    var autoPrint by remember(currentConfig) { mutableStateOf(currentConfig.autoPrintOnDispatch) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = KdsSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, KdsCardBorder),
            modifier = Modifier
                .widthIn(min = 480.dp, max = 560.dp)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Konfigurasi Printer Stasiun",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Atur alamat target Bluetooth / LAN ESC/POS untuk tiap stasiun",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Pemilihan Stasiun
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WorkStation.entries.forEach { station ->
                        val isSelected = station == selectedStation
                        Surface(
                            onClick = { selectedStation = station },
                            color = if (isSelected) BaristaAmber.copy(alpha = 0.2f) else KdsSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) BaristaAmber else KdsCardBorder
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = station.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tipe Printer
                Text(text = "Tipe Printer ESC/POS", fontSize = 13.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        PrinterType.MOCK to "Mock / Virtual",
                        PrinterType.BLUETOOTH_ESC_POS to "Bluetooth SPP",
                        PrinterType.NETWORK_LAN_ESC_POS to "Network LAN/WiFi"
                    ).forEach { (type, label) ->
                        val isSelected = printerType == type
                        Surface(
                            onClick = { printerType = type },
                            color = if (isSelected) NormalGreen.copy(alpha = 0.2f) else KdsSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) NormalGreen else KdsCardBorder
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Target Address Input
                val addressLabel = when (printerType) {
                    PrinterType.BLUETOOTH_ESC_POS -> "Alamat MAC Bluetooth (cth: 00:11:22:33:44:55)"
                    PrinterType.NETWORK_LAN_ESC_POS -> "IP & Port Printer (cth: 192.168.1.200:9100)"
                    else -> "Target Simulator / Mock"
                }

                OutlinedTextField(
                    value = targetAddress,
                    onValueChange = { targetAddress = it },
                    label = { Text(addressLabel, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = BaristaAmber,
                        unfocusedBorderColor = KdsCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Auto-print Toggle
                Surface(
                    color = KdsSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Auto-print Tiket Stasiun",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Otomatis cetak tiket slip saat kasir menyelesaikan checkout",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        Switch(
                            checked = autoPrint,
                            onCheckedChange = { autoPrint = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NormalGreen
                            )
                        )
                    }
                }

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
                            val updatedConfig = currentConfig.copy(
                                printerType = printerType,
                                targetAddress = targetAddress,
                                autoPrintOnDispatch = autoPrint
                            )
                            onSave(updatedConfig)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BaristaAmber),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Simpan Konfigurasi", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
