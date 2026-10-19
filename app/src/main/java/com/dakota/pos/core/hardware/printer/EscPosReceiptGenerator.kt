package com.dakota.pos.core.hardware.printer

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

/**
 * Generator perintah ESC/POS biner untuk printer thermal kasir (58mm / 80mm).
 * Menyediakan byte array siap kirim via Bluetooth, USB, atau TCP/IP socket.
 */
object EscPosReceiptGenerator {

    // Perintah dasar ESC/POS
    private val ESC: Byte = 0x1B
    private val GS: Byte = 0x1D
    private val LF: Byte = 0x0A

    // Inisialisasi printer
    val CMD_INIT = byteArrayOf(ESC, '@'.code.toByte())

    // Perataan teks (Alignment)
    val CMD_ALIGN_LEFT = byteArrayOf(ESC, 'a'.code.toByte(), 0)
    val CMD_ALIGN_CENTER = byteArrayOf(ESC, 'a'.code.toByte(), 1)
    val CMD_ALIGN_RIGHT = byteArrayOf(ESC, 'a'.code.toByte(), 2)

    // Format teks (Bold / Emphasized)
    val CMD_BOLD_ON = byteArrayOf(ESC, 'E'.code.toByte(), 1)
    val CMD_BOLD_OFF = byteArrayOf(ESC, 'E'.code.toByte(), 0)

    // Ukuran font (GS ! n)
    val CMD_FONT_NORMAL = byteArrayOf(GS, '!'.code.toByte(), 0x00)
    val CMD_FONT_DOUBLE_HEIGHT = byteArrayOf(GS, '!'.code.toByte(), 0x01)
    val CMD_FONT_DOUBLE_WIDTH = byteArrayOf(GS, '!'.code.toByte(), 0x10)
    val CMD_FONT_DOUBLE_SIZE = byteArrayOf(GS, '!'.code.toByte(), 0x11)

    // Buka Cash Drawer (Kick pulse: Pin 2 atau Pin 5)
    val CMD_OPEN_DRAWER = byteArrayOf(ESC, 'p'.code.toByte(), 0, 50, 50)

    // Potong kertas (Partial cut / Full cut)
    val CMD_CUT_PAPER = byteArrayOf(GS, 'V'.code.toByte(), 1)

    /**
     * Mengubah data struk [PrintReceiptData] menjadi rentetan byte ESC/POS siap kirim ke printer.
     */
    fun generateBytes(receipt: PrintReceiptData, charset: Charset = Charsets.ISO_8859_1): ByteArray {
        val stream = ByteArrayOutputStream()

        fun write(bytes: ByteArray) = stream.write(bytes)
        fun writeText(text: String) = stream.write(text.toByteArray(charset))
        fun lineFeed(lines: Int = 1) = repeat(lines) { stream.write(LF.toInt()) }

        // Inisialisasi
        write(CMD_INIT)

        // 1. Header Outlet (Center & Double Size)
        write(CMD_ALIGN_CENTER)
        write(CMD_BOLD_ON)
        write(CMD_FONT_DOUBLE_HEIGHT)
        writeText(receipt.outletName.uppercase())
        lineFeed()
        write(CMD_FONT_NORMAL)
        write(CMD_BOLD_OFF)

        if (!receipt.outletAddress.isNullOrBlank()) {
            writeText(receipt.outletAddress)
            lineFeed()
        }
        writeText("================================")
        lineFeed()

        // 2, 3, 4. Order info (Left aligned)
        write(CMD_ALIGN_LEFT)
        writeText("No. Order : ${receipt.orderNumber}")
        lineFeed()
        writeText("Waktu     : ${receipt.dateTime}")
        lineFeed()
        writeText("Kasir     : ${receipt.cashierName}")
        lineFeed()
        writeText("--------------------------------")
        lineFeed()

        // 5. Item list
        receipt.items.forEach { item ->
            write(CMD_BOLD_ON)
            writeText(item.name)
            write(CMD_BOLD_OFF)
            lineFeed()

            if (!item.variantModifier.isNullOrBlank()) {
                writeText("  (${item.variantModifier})")
                lineFeed()
            }

            val qtyText = "  ${item.quantity} x ${receipt.currencyString(item.unitPrice)}"
            val subtotalText = receipt.currencyString(item.subtotal)
            writeText(receipt.twoColumns(qtyText, subtotalText, 32))
            lineFeed()
        }

        writeText("--------------------------------")
        lineFeed()

        // 6, 7. Subtotal, Discount, Tax, Total
        writeText(receipt.twoColumns("Subtotal", receipt.currencyString(receipt.subtotal), 32))
        lineFeed()

        if (receipt.discount > 0) {
            writeText(receipt.twoColumns("Diskon", "-${receipt.currencyString(receipt.discount)}", 32))
            lineFeed()
        }

        if (receipt.tax > 0) {
            writeText(receipt.twoColumns("Pajak", receipt.currencyString(receipt.tax), 32))
            lineFeed()
        }

        writeText("================================")
        lineFeed()

        // Total (Bold)
        write(CMD_BOLD_ON)
        write(CMD_FONT_DOUBLE_HEIGHT)
        writeText(receipt.twoColumns("TOTAL", receipt.currencyString(receipt.total), 32))
        lineFeed()
        write(CMD_FONT_NORMAL)
        write(CMD_BOLD_OFF)

        writeText("================================")
        lineFeed()

        // 8. Payment Details
        writeText(receipt.twoColumns("Metode", receipt.paymentMethod, 32))
        lineFeed()

        if (receipt.cashReceived != null) {
            writeText(receipt.twoColumns("Tunai", receipt.currencyString(receipt.cashReceived), 32))
            lineFeed()
            val change = receipt.change ?: (receipt.cashReceived - receipt.total)
            writeText(receipt.twoColumns("Kembalian", receipt.currencyString(change), 32))
            lineFeed()
        }

        if (!receipt.referenceNumber.isNullOrBlank()) {
            writeText(receipt.twoColumns("No. Ref", receipt.referenceNumber, 32))
            lineFeed()
        }

        writeText("--------------------------------")
        lineFeed()

        // 9. Footer
        write(CMD_ALIGN_CENTER)
        if (!receipt.footerMessage.isNullOrBlank()) {
            writeText(receipt.footerMessage)
            lineFeed()
        }
        if (!receipt.footerSocial.isNullOrBlank()) {
            writeText(receipt.footerSocial)
            lineFeed()
        }
        writeText("Simpan struk sebagai bukti sah")
        lineFeed(3) // Feed ekstra sebelum potong kertas

        // Potong kertas
        write(CMD_CUT_PAPER)

        return stream.toByteArray()
    }

    private fun PrintReceiptData.currencyString(amount: Double): String {
        return "Rp %,.0f".format(amount)
    }

    private fun PrintReceiptData.twoColumns(left: String, right: String, width: Int): String {
        val totalSpace = width - left.length - right.length
        return if (totalSpace >= 1) {
            left + " ".repeat(totalSpace) + right
        } else {
            val maxLeft = width - right.length - 1
            left.take(maxLeft) + " " + right
        }
    }
}
