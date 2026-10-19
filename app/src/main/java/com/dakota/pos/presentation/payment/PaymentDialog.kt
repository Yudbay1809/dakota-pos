package com.dakota.pos.presentation.payment

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dakota.pos.domain.payment.PaymentMethodType
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentDialog(
    totalAmount: Double,
    selectedMethod: PaymentMethodType,
    cashReceivedInput: String,
    cashReceivedAmount: Double,
    changeAmount: Double,
    referenceNumber: String,
    isProcessing: Boolean,
    errorMessage: String?,
    onSelectMethod: (PaymentMethodType) -> Unit,
    onCashInputChange: (String) -> Unit,
    onQuickCashSelected: (Double) -> Unit,
    onReferenceNumberChange: (String) -> Unit,
    onConfirmPayment: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .fillMaxHeight(0.88f),
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
                        Text(
                            text = "Pembayaran Pesanan",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = PosTextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pilih metode pembayaran dan masukkan detail transaksi",
                            style = MaterialTheme.typography.bodySmall,
                            color = PosTextSecondaryDark
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = PosTextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = PosBorderDark)
                Spacer(modifier = Modifier.height(16.dp))

                // Error Banner if any
                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = StatusError.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, StatusError.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = StatusError,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Two columns: Left (Method selector) | Right (Method details & calculation)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // LEFT COLUMN: Payment Methods
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "METODE PEMBAYARAN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = CoffeeLatte
                        )

                        val methods = listOf(
                            PaymentMethodItem(PaymentMethodType.CASH, "Tunai (Cash)", Icons.Default.Money),
                            PaymentMethodItem(PaymentMethodType.QRIS, "QRIS Dinamis", Icons.Default.QrCodeScanner),
                            PaymentMethodItem(PaymentMethodType.DEBIT_CARD, "Kartu Debit", Icons.Default.CreditCard),
                            PaymentMethodItem(PaymentMethodType.CREDIT_CARD, "Kartu Kredit", Icons.Default.CreditCard),
                            PaymentMethodItem(PaymentMethodType.BANK_TRANSFER, "Transfer Bank", Icons.Default.AccountBalance),
                            PaymentMethodItem(PaymentMethodType.E_WALLET, "E-Wallet", Icons.Default.AccountBalanceWallet)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(methods.size) { index ->
                                val item = methods[index]
                                val isSelected = item.type == selectedMethod
                                val bgColor by animateColorAsState(
                                    if (isSelected) CoffeeCaramel.copy(alpha = 0.15f) else PosSurfaceElevatedDark,
                                    label = "method_bg"
                                )
                                val borderColor by animateColorAsState(
                                    if (isSelected) CoffeeCaramel else PosBorderDark,
                                    label = "method_border"
                                )
                                val contentColor by animateColorAsState(
                                    if (isSelected) CoffeeCaramel else PosTextSecondaryDark,
                                    label = "method_text"
                                )

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isProcessing) { onSelectMethod(item.type) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = bgColor,
                                    border = BorderStroke(1.5.dp, borderColor)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) CoffeeCaramel.copy(alpha = 0.2f)
                                                    else PosSurfaceDark
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                item.icon,
                                                contentDescription = null,
                                                tint = contentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) PosTextPrimaryDark else PosTextSecondaryDark
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // RIGHT COLUMN: Method Specifics & Calculations
                    Surface(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(16.dp),
                        color = PosSurfaceElevatedDark,
                        border = BorderStroke(1.dp, PosBorderDark)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                        ) {
                            // Total Tagihan Header
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PosSurfaceDark,
                                border = BorderStroke(1.dp, PosBorderDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Total Tagihan",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = PosTextSecondaryDark
                                        )
                                        Text(
                                            formatRupiah(totalAmount),
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = CoffeeCaramel
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Interactive Area based on Payment Method
                            when (selectedMethod) {
                                PaymentMethodType.CASH -> {
                                    CashPaymentContent(
                                        totalAmount = totalAmount,
                                        cashReceivedInput = cashReceivedInput,
                                        cashReceivedAmount = cashReceivedAmount,
                                        changeAmount = changeAmount,
                                        isProcessing = isProcessing,
                                        onCashInputChange = onCashInputChange,
                                        onQuickCashSelected = onQuickCashSelected
                                    )
                                }
                                PaymentMethodType.QRIS -> {
                                    QrisPaymentContent(
                                        totalAmount = totalAmount,
                                        referenceNumber = referenceNumber,
                                        onReferenceNumberChange = onReferenceNumberChange
                                    )
                                }
                                else -> {
                                    CardPaymentContent(
                                        methodType = selectedMethod,
                                        referenceNumber = referenceNumber,
                                        onReferenceNumberChange = onReferenceNumberChange
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Bottom Confirm Button
                            val isConfirmEnabled = when (selectedMethod) {
                                PaymentMethodType.CASH -> cashReceivedAmount >= totalAmount && !isProcessing
                                else -> !isProcessing
                            }

                            Button(
                                onClick = onConfirmPayment,
                                enabled = isConfirmEnabled,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CoffeeCaramel,
                                    disabledContainerColor = PosSurfaceDark
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = CoffeeEspressoDark,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "Memproses...",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = CoffeeEspressoDark
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isConfirmEnabled) CoffeeEspressoDark else PosTextMutedDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when (selectedMethod) {
                                            PaymentMethodType.CASH -> "Selesaikan Pembayaran Tunai"
                                            PaymentMethodType.QRIS -> "Konfirmasi Pembayaran QRIS"
                                            else -> "Konfirmasi Pembayaran"
                                        },
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isConfirmEnabled) CoffeeEspressoDark else PosTextMutedDark
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

// ============================================================================
//  CASH CONTENT (Input + Quick Shortcuts + Realtime Change Calculation)
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CashPaymentContent(
    totalAmount: Double,
    cashReceivedInput: String,
    cashReceivedAmount: Double,
    changeAmount: Double,
    isProcessing: Boolean,
    onCashInputChange: (String) -> Unit,
    onQuickCashSelected: (Double) -> Unit
) {
    Column {
        Text(
            "Uang Tunai Diterima",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = PosTextPrimaryDark
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = cashReceivedInput,
            onValueChange = { input ->
                val filtered = input.filter { it.isDigit() }
                onCashInputChange(filtered)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = { Text("Contoh: 50000", color = PosTextMutedDark) },
            prefix = {
                Text(
                    "Rp ",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CoffeeCaramel
                )
            },
            textStyle = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = PosTextPrimaryDark
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PosSurfaceDark,
                unfocusedContainerColor = PosSurfaceDark,
                focusedBorderColor = CoffeeCaramel,
                unfocusedBorderColor = PosBorderDark,
                cursorColor = CoffeeCaramel
            ),
            singleLine = true,
            enabled = !isProcessing
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Cash Buttons
        Text(
            "Nominal Cepat",
            style = MaterialTheme.typography.labelSmall.copy(color = PosTextSecondaryDark)
        )
        Spacer(modifier = Modifier.height(6.dp))

        val quickNominals = generateQuickNominals(totalAmount)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Uang Pas button
            QuickButton(
                label = "Uang Pas",
                amount = totalAmount,
                isSelected = cashReceivedAmount == totalAmount,
                onClick = { onQuickCashSelected(totalAmount) }
            )

            quickNominals.forEach { nominal ->
                QuickButton(
                    label = formatRupiahShort(nominal),
                    amount = nominal,
                    isSelected = cashReceivedAmount == nominal,
                    onClick = { onQuickCashSelected(nominal) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Kembalian Card
        val isExactOrMore = cashReceivedAmount >= totalAmount
        val statusBg = if (cashReceivedAmount == 0.0) {
            PosSurfaceDark
        } else if (isExactOrMore) {
            StatusSuccess.copy(alpha = 0.12f)
        } else {
            StatusError.copy(alpha = 0.12f)
        }

        val statusBorder = if (cashReceivedAmount == 0.0) {
            PosBorderDark
        } else if (isExactOrMore) {
            StatusSuccess.copy(alpha = 0.35f)
        } else {
            StatusError.copy(alpha = 0.35f)
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = statusBg,
            border = BorderStroke(1.dp, statusBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (cashReceivedAmount == 0.0) "Kembalian"
                        else if (isExactOrMore) "Kembalian"
                        else "Kekurangan",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (cashReceivedAmount == 0.0) PosTextSecondaryDark
                        else if (isExactOrMore) StatusSuccess
                        else StatusError
                    )
                    Text(
                        text = if (cashReceivedAmount == 0.0) "Rp 0"
                        else if (isExactOrMore) formatRupiah(changeAmount)
                        else "-${formatRupiah(totalAmount - cashReceivedAmount)}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (cashReceivedAmount == 0.0) PosTextPrimaryDark
                        else if (isExactOrMore) StatusSuccess
                        else StatusError
                    )
                }

                if (isExactOrMore && cashReceivedAmount > 0.0) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusSuccess,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickButton(
    label: String,
    amount: Double,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) CoffeeCaramel else PosSurfaceDark,
        border = BorderStroke(1.dp, if (isSelected) CoffeeCaramel else PosBorderDark)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = if (isSelected) CoffeeEspressoDark else PosTextPrimaryDark,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

// ============================================================================
//  QRIS CONTENT
// ============================================================================

@Composable
private fun QrisPaymentContent(
    totalAmount: Double,
    referenceNumber: String,
    onReferenceNumberChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Scan QRIS dengan BCA, GoPay, OVO, Dana, ShopeePay",
            style = MaterialTheme.typography.bodySmall,
            color = PosTextSecondaryDark,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Visual QR Placeholder Frame
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(2.dp, CoffeeCaramel),
            modifier = Modifier.size(170.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // QRIS header tag
                Surface(
                    color = Color(0xFFC02626),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        "QRIS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }

                Icon(
                    Icons.Default.QrCode,
                    contentDescription = "QRIS Code",
                    tint = Color.Black,
                    modifier = Modifier.size(100.dp)
                )

                Text(
                    "DAKOTA COFFEE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Total: ${formatRupiah(totalAmount)}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = CoffeeCaramel
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = referenceNumber,
            onValueChange = onReferenceNumberChange,
            label = { Text("No. Referensi / RRN (Opsional)", color = PosTextMutedDark) },
            placeholder = { Text("Otomatis digenerate jika kosong", color = PosTextMutedDark) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PosSurfaceDark,
                unfocusedContainerColor = PosSurfaceDark,
                focusedBorderColor = CoffeeCaramel,
                unfocusedBorderColor = PosBorderDark,
                cursorColor = CoffeeCaramel,
                focusedTextColor = PosTextPrimaryDark,
                unfocusedTextColor = PosTextPrimaryDark
            )
        )
    }
}

// ============================================================================
//  CARD / TRANSFER / E-WALLET CONTENT
// ============================================================================

@Composable
private fun CardPaymentContent(
    methodType: PaymentMethodType,
    referenceNumber: String,
    onReferenceNumberChange: (String) -> Unit
) {
    Column {
        val title = when (methodType) {
            PaymentMethodType.DEBIT_CARD -> "Pembayaran Kartu Debit EDC"
            PaymentMethodType.CREDIT_CARD -> "Pembayaran Kartu Kredit EDC"
            PaymentMethodType.BANK_TRANSFER -> "Transfer Bank (BCA / Mandiri / BRI)"
            PaymentMethodType.E_WALLET -> "E-Wallet (OVO / GoPay / Dana / LinkAja)"
            else -> "Pembayaran Non-Tunai"
        }

        Text(
            title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = PosTextPrimaryDark
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            "Gesek atau masukkan kartu ke mesin EDC / minta bukti transfer pelanggan, lalu catat nomor approval/referensi bila ada.",
            style = MaterialTheme.typography.bodySmall,
            color = PosTextSecondaryDark
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = referenceNumber,
            onValueChange = onReferenceNumberChange,
            label = { Text("No. Referensi / Approval Code", color = PosTextMutedDark) },
            placeholder = { Text("Contoh: 8392019483", color = PosTextMutedDark) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PosSurfaceDark,
                unfocusedContainerColor = PosSurfaceDark,
                focusedBorderColor = CoffeeCaramel,
                unfocusedBorderColor = PosBorderDark,
                cursorColor = CoffeeCaramel,
                focusedTextColor = PosTextPrimaryDark,
                unfocusedTextColor = PosTextPrimaryDark
            )
        )
    }
}

// ============================================================================
//  HELPERS
// ============================================================================

private data class PaymentMethodItem(
    val type: PaymentMethodType,
    val label: String,
    val icon: ImageVector
)

private fun generateQuickNominals(total: Double): List<Double> {
    val list = mutableListOf<Double>()
    val baseNominals = listOf(20000.0, 50000.0, 100000.0, 200000.0, 500000.0)

    // Add standard nominals greater than total
    baseNominals.filter { it > total }.take(3).forEach {
        list.add(it)
    }

    // Next round up (e.g. 44k -> 50k, 73k -> 80k or 100k)
    val round10k = (Math.ceil(total / 10000.0) * 10000.0)
    if (round10k > total && !list.contains(round10k)) {
        list.add(0, round10k)
    }

    val round50k = (Math.ceil(total / 50000.0) * 50000.0)
    if (round50k > total && !list.contains(round50k)) {
        list.add(round50k)
    }

    return list.distinct().sorted().take(4)
}

private fun formatRupiah(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("in", "ID"))
    return "Rp " + formatter.format(amount.toLong())
}

private fun formatRupiahShort(amount: Double): String {
    return if (amount >= 1000) {
        "${(amount / 1000).toInt()}k"
    } else {
        amount.toInt().toString()
    }
}
