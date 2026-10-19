package com.dakota.pos.core.hardware.printer

import com.dakota.pos.domain.model.kds.KitchenOrder
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StationTicketGenerator {

    private val ESC: Byte = 0x1B
    private val GS: Byte = 0x1D
    private val LF: Byte = 0x0A

    val CMD_INIT = byteArrayOf(ESC, '@'.code.toByte())
    val CMD_ALIGN_LEFT = byteArrayOf(ESC, 'a'.code.toByte(), 0)
    val CMD_ALIGN_CENTER = byteArrayOf(ESC, 'a'.code.toByte(), 1)
    val CMD_ALIGN_RIGHT = byteArrayOf(ESC, 'a'.code.toByte(), 2)
    val CMD_BOLD_ON = byteArrayOf(ESC, 'E'.code.toByte(), 1)
    val CMD_BOLD_OFF = byteArrayOf(ESC, 'E'.code.toByte(), 0)
    val CMD_FONT_NORMAL = byteArrayOf(GS, '!'.code.toByte(), 0x00)
    val CMD_FONT_DOUBLE_HEIGHT = byteArrayOf(GS, '!'.code.toByte(), 0x01)
    val CMD_FONT_DOUBLE_SIZE = byteArrayOf(GS, '!'.code.toByte(), 0x11)
    val CMD_CUT_PAPER = byteArrayOf(GS, 'V'.code.toByte(), 1)

    fun generateTicketBytes(kitchenOrder: KitchenOrder, charset: Charset = Charsets.ISO_8859_1): ByteArray {
        val stream = ByteArrayOutputStream()

        fun write(bytes: ByteArray) = stream.write(bytes)
        fun writeText(text: String) = stream.write(text.toByteArray(charset))
        fun lineFeed(lines: Int = 1) = repeat(lines) { stream.write(LF.toInt()) }

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID"))
        val dateStr = sdf.format(Date(kitchenOrder.orderTime))

        // Inisialisasi
        write(CMD_INIT)

        // Header Stasiun
        write(CMD_ALIGN_CENTER)
        write(CMD_BOLD_ON)
        write(CMD_FONT_DOUBLE_SIZE)
        writeText("*** ${kitchenOrder.station.displayName.uppercase()} ***")
        lineFeed()
        write(CMD_FONT_NORMAL)
        write(CMD_BOLD_OFF)
        writeText("ORDER #${kitchenOrder.orderNumber}")
        lineFeed()
        writeText("Waktu: $dateStr")
        lineFeed()
        writeText("Kasir: ${kitchenOrder.cashierName}")
        lineFeed()

        // Garis Pembatas
        write(CMD_ALIGN_LEFT)
        writeText("----------------------------------------")
        lineFeed()

        // Item List (Ukuran besar & jelas untuk barista/koki)
        kitchenOrder.items.forEach { item ->
            write(CMD_BOLD_ON)
            write(CMD_FONT_DOUBLE_HEIGHT)
            writeText("${item.quantity}x ${item.productName}")
            lineFeed()
            write(CMD_FONT_NORMAL)
            write(CMD_BOLD_OFF)

            if (!item.variantName.isNullOrBlank()) {
                writeText("   Varian: ${item.variantName}")
                lineFeed()
            }
            if (!item.modifiersSummary.isNullOrBlank()) {
                writeText("   Mod: ${item.modifiersSummary}")
                lineFeed()
            }
            if (!item.notes.isNullOrBlank()) {
                write(CMD_BOLD_ON)
                writeText("   NOTE: ${item.notes}")
                lineFeed()
                write(CMD_BOLD_OFF)
            }
            lineFeed()
        }

        // Catatan Khusus Pelanggan
        if (!kitchenOrder.customerNotes.isNullOrBlank()) {
            writeText("----------------------------------------")
            lineFeed()
            write(CMD_BOLD_ON)
            writeText("CATATAN ORDER: ${kitchenOrder.customerNotes}")
            lineFeed()
            write(CMD_BOLD_OFF)
        }

        // Footer
        writeText("----------------------------------------")
        lineFeed(2)
        write(CMD_CUT_PAPER)

        return stream.toByteArray()
    }

    fun generatePlainText(kitchenOrder: KitchenOrder): String {
        val sb = StringBuilder()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID"))
        val dateStr = sdf.format(Date(kitchenOrder.orderTime))

        sb.appendLine("========================================")
        sb.appendLine("       ${kitchenOrder.station.displayName.uppercase()}")
        sb.appendLine("========================================")
        sb.appendLine("Order #${kitchenOrder.orderNumber}")
        sb.appendLine("Waktu: $dateStr")
        sb.appendLine("Kasir: ${kitchenOrder.cashierName}")
        sb.appendLine("----------------------------------------")
        kitchenOrder.items.forEach { item ->
            sb.appendLine("${item.quantity}x ${item.productName}")
            if (!item.variantName.isNullOrBlank()) sb.appendLine("   Varian: ${item.variantName}")
            if (!item.modifiersSummary.isNullOrBlank()) sb.appendLine("   Mod: ${item.modifiersSummary}")
            if (!item.notes.isNullOrBlank()) sb.appendLine("   NOTE: ${item.notes}")
        }
        if (!kitchenOrder.customerNotes.isNullOrBlank()) {
            sb.appendLine("----------------------------------------")
            sb.appendLine("CATATAN: ${kitchenOrder.customerNotes}")
        }
        sb.appendLine("========================================")
        return sb.toString()
    }
}
