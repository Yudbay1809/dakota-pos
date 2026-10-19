package com.dakota.pos.domain.usecase.payment

import com.dakota.pos.core.hardware.printer.PrintReceiptData
import com.dakota.pos.core.hardware.printer.PrintReceiptItem
import com.dakota.pos.core.hardware.printer.PrintResult
import com.dakota.pos.core.hardware.printer.PrinterInterface
import com.dakota.pos.core.hardware.printer.ReceiptTextFormatter
import com.dakota.pos.domain.model.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrintReceiptUseCase(
    private val printer: PrinterInterface,
    private val outletName: String = "Dakota Coffee Shop",
    private val outletAddress: String = "Jl. Kopi Dakota No. 1, Jakarta"
) {

    data class PrintExecutionResult(
        val printResult: PrintResult,
        val receiptData: PrintReceiptData,
        val formattedText: String
    )

    suspend operator fun invoke(
        order: Order,
        cashReceived: Double? = null,
        change: Double? = null,
        referenceNumber: String? = null
    ): PrintExecutionResult {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(order.createdAt))

        val receiptItems = order.items.map { item ->
            PrintReceiptItem(
                name = item.productName,
                variantModifier = listOfNotNull(
                    item.variantName,
                    item.selectedModifiersSummary
                ).joinToString(", ").takeIf { it.isNotBlank() },
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                subtotal = item.subtotal
            )
        }

        val receiptData = PrintReceiptData(
            outletName = outletName,
            outletAddress = outletAddress,
            orderNumber = order.orderNumber,
            dateTime = formattedDate,
            cashierName = order.cashierName,
            items = receiptItems,
            subtotal = order.subtotal,
            discount = order.discount,
            tax = order.tax,
            total = order.total,
            paymentMethod = order.paymentMethod ?: "CASH",
            cashReceived = cashReceived,
            change = change,
            referenceNumber = referenceNumber,
            footerMessage = "Terima kasih atas kunjungan Anda!",
            footerSocial = "Instagram: @dakotacoffee.id"
        )

        val formattedText = ReceiptTextFormatter.formatReceipt(receiptData)
        val printResult = printer.printReceipt(receiptData)

        return PrintExecutionResult(
            printResult = printResult,
            receiptData = receiptData,
            formattedText = formattedText
        )
    }

    suspend fun openCashDrawer(): Boolean {
        return printer.openCashDrawer()
    }
}
