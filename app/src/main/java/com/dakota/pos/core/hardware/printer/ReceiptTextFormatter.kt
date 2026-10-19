package com.dakota.pos.core.hardware.printer

import java.text.NumberFormat
import java.util.Locale

/**
 * Generator teks struk kasir dengan format monospace standar 32 karakter
 * (kompatibel printer thermal 58mm & 80mm).
 * Mengimplementasikan 9 komponen minimal sesuai spesifikasi Fase 06.
 */
object ReceiptTextFormatter {

    private const val PAPER_WIDTH = 32

    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("in", "ID"))
        return "Rp " + formatter.format(amount.toLong())
    }

    private fun center(text: String, width: Int = PAPER_WIDTH): String {
        if (text.length >= width) return text
        val pad = (width - text.length) / 2
        return " ".repeat(pad) + text
    }

    private fun line(char: Char = '-', width: Int = PAPER_WIDTH): String {
        return char.toString().repeat(width)
    }

    private fun twoColumns(left: String, right: String, width: Int = PAPER_WIDTH): String {
        val totalSpace = width - left.length - right.length
        return if (totalSpace >= 1) {
            left + " ".repeat(totalSpace) + right
        } else {
            left + " " + right
        }
    }

    /**
     * Membangun format teks struk lengkap berdasarkan PrintReceiptData.
     */
    fun formatReceipt(receipt: PrintReceiptData): String {
        val sb = StringBuilder()

        // 1. Header: Nama Outlet & Alamat
        sb.appendLine(center(receipt.outletName.uppercase()))
        if (!receipt.outletAddress.isNullOrBlank()) {
            sb.appendLine(center(receipt.outletAddress))
        }
        sb.appendLine(line('='))

        // 2, 3, 4. Order info: No Order, Waktu, Kasir
        sb.appendLine(twoColumns("No. Order :", receipt.orderNumber))
        sb.appendLine(twoColumns("Waktu     :", receipt.dateTime))
        sb.appendLine(twoColumns("Kasir     :", receipt.cashierName))
        sb.appendLine(line('-'))

        // 5. Detail Items
        receipt.items.forEach { item ->
            // Baris nama produk
            sb.appendLine(item.name)

            // Varian atau modifier jika ada
            if (!item.variantModifier.isNullOrBlank()) {
                sb.appendLine("  (${item.variantModifier})")
            }

            // Qty x Harga satuan   Subtotal
            val qtyPrice = "  ${item.quantity} x ${formatCurrency(item.unitPrice)}"
            val subtotal = formatCurrency(item.subtotal)
            sb.appendLine(twoColumns(qtyPrice, subtotal))
        }

        sb.appendLine(line('-'))

        // 6 & 7. Subtotal, Diskon, Pajak, Total Pembayaran
        sb.appendLine(twoColumns("Subtotal", formatCurrency(receipt.subtotal)))

        if (receipt.discount > 0) {
            sb.appendLine(twoColumns("Diskon", "-${formatCurrency(receipt.discount)}"))
        }

        if (receipt.tax > 0) {
            sb.appendLine(twoColumns("PB1 / Pajak", formatCurrency(receipt.tax)))
        }

        sb.appendLine(line('='))
        sb.appendLine(twoColumns("TOTAL", formatCurrency(receipt.total)))
        sb.appendLine(line('='))

        // 8. Metode Pembayaran & Detail Bayar (Cash / QRIS / Kartu)
        sb.appendLine(twoColumns("Metode Bayar", receipt.paymentMethod))

        if (receipt.cashReceived != null) {
            sb.appendLine(twoColumns("Tunai Diterima", formatCurrency(receipt.cashReceived)))
            val changeAmount = receipt.change ?: (receipt.cashReceived - receipt.total)
            sb.appendLine(twoColumns("Kembalian", formatCurrency(changeAmount)))
        }

        if (!receipt.referenceNumber.isNullOrBlank()) {
            sb.appendLine(twoColumns("No. Ref", receipt.referenceNumber))
        }

        sb.appendLine(line('-'))

        // 9. Footer Struk: Pesan Terima Kasih & Media Sosial
        if (!receipt.footerMessage.isNullOrBlank()) {
            sb.appendLine(center(receipt.footerMessage))
        }
        if (!receipt.footerSocial.isNullOrBlank()) {
            sb.appendLine(center(receipt.footerSocial))
        }
        sb.appendLine(center("Simpan struk sebagai bukti sah"))
        sb.appendLine(line('='))

        return sb.toString()
    }
}
