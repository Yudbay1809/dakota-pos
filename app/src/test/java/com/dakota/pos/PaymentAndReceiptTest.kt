package com.dakota.pos

import com.dakota.pos.core.hardware.printer.EscPosReceiptGenerator
import com.dakota.pos.core.hardware.printer.MockPrinter
import com.dakota.pos.core.hardware.printer.PrintReceiptData
import com.dakota.pos.core.hardware.printer.PrintReceiptItem
import com.dakota.pos.core.hardware.printer.PrintResult
import com.dakota.pos.core.hardware.printer.ReceiptTextFormatter
import com.dakota.pos.domain.model.Cart
import com.dakota.pos.domain.model.CartItem
import com.dakota.pos.domain.model.Category
import com.dakota.pos.domain.model.Order
import com.dakota.pos.domain.model.OrderItem
import com.dakota.pos.domain.model.OrderStatus
import com.dakota.pos.domain.model.Product
import com.dakota.pos.domain.payment.CashPaymentProcessor
import com.dakota.pos.domain.payment.GenericCardPaymentProcessor
import com.dakota.pos.domain.payment.MockQrisPaymentProcessor
import com.dakota.pos.domain.payment.PaymentMethodType
import com.dakota.pos.domain.payment.PaymentProcessorRegistry
import com.dakota.pos.domain.payment.PaymentRequest
import com.dakota.pos.domain.payment.PaymentResult
import com.dakota.pos.domain.repository.OrderRepository
import com.dakota.pos.domain.usecase.payment.PrintReceiptUseCase
import com.dakota.pos.domain.usecase.payment.ProcessPaymentUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PaymentAndReceiptTest {

    private lateinit var cashProcessor: CashPaymentProcessor
    private lateinit var qrisProcessor: MockQrisPaymentProcessor
    private lateinit var cardProcessor: GenericCardPaymentProcessor
    private lateinit var registry: PaymentProcessorRegistry
    private lateinit var mockPrinter: MockPrinter

    @Before
    fun setup() {
        cashProcessor = CashPaymentProcessor()
        qrisProcessor = MockQrisPaymentProcessor()
        cardProcessor = GenericCardPaymentProcessor(PaymentMethodType.DEBIT_CARD)
        registry = PaymentProcessorRegistry(
            cashProcessor = cashProcessor,
            qrisProcessor = qrisProcessor
        )
        mockPrinter = MockPrinter()
    }

    // ========================================================================
    // 1. CASH PAYMENT & CHANGE CALCULATION TESTS
    // ========================================================================

    @Test
    fun `cash payment exact amount produces zero change`() = runTest {
        val request = PaymentRequest(
            orderId = "ord-1",
            totalAmount = 44000.0,
            method = PaymentMethodType.CASH,
            cashReceived = 44000.0
        )

        val result = cashProcessor.processPayment(request)
        assertTrue(result is PaymentResult.Success)
        val success = result as PaymentResult.Success
        assertEquals(0.0, success.change, 0.001)
        assertEquals(44000.0, success.totalAmount, 0.001)
        assertEquals(PaymentMethodType.CASH, success.method)
    }

    @Test
    fun `cash payment overpayment calculates correct change`() = runTest {
        val request = PaymentRequest(
            orderId = "ord-2",
            totalAmount = 44000.0,
            method = PaymentMethodType.CASH,
            cashReceived = 50000.0
        )

        val result = cashProcessor.processPayment(request)
        assertTrue(result is PaymentResult.Success)
        val success = result as PaymentResult.Success
        assertEquals(6000.0, success.change, 0.001)
    }

    @Test
    fun `cash payment underpayment returns failure with error message`() = runTest {
        val request = PaymentRequest(
            orderId = "ord-3",
            totalAmount = 44000.0,
            method = PaymentMethodType.CASH,
            cashReceived = 40000.0
        )

        val result = cashProcessor.processPayment(request)
        assertTrue(result is PaymentResult.Failure)
        val failure = result as PaymentResult.Failure
        assertTrue(failure.errorMessage.contains("kurang", ignoreCase = true))
    }

    @Test
    fun `cash payment without cashReceived returns failure`() = runTest {
        val request = PaymentRequest(
            orderId = "ord-4",
            totalAmount = 44000.0,
            method = PaymentMethodType.CASH,
            cashReceived = null
        )

        val result = cashProcessor.processPayment(request)
        assertTrue(result is PaymentResult.Failure)
    }

    // ========================================================================
    // 2. QRIS & CARD PAYMENT TESTS
    // ========================================================================

    @Test
    fun `qris processor generates payload and reference`() = runTest {
        val payload = qrisProcessor.generateDynamicQrisPayload("ord-qris-1", 35000.0)
        assertTrue(payload.startsWith("000201"))
        assertTrue(payload.contains("DAKOTA COFFEE SHOP", ignoreCase = true))

        val request = PaymentRequest(
            orderId = "ord-qris-1",
            totalAmount = 35000.0,
            method = PaymentMethodType.QRIS
        )

        val result = qrisProcessor.processPayment(request)
        assertTrue(result is PaymentResult.Success)
        val success = result as PaymentResult.Success
        assertNotNull(success.transactionReference)
        assertTrue(success.transactionReference!!.startsWith("QRIS-"))
        assertEquals(0.0, success.change, 0.001)
    }

    @Test
    fun `card and e-wallet processors succeed with reference codes`() = runTest {
        val debitProcessor = GenericCardPaymentProcessor(PaymentMethodType.DEBIT_CARD)
        val request = PaymentRequest(
            orderId = "ord-debit-1",
            totalAmount = 65000.0,
            method = PaymentMethodType.DEBIT_CARD,
            referenceNumber = "REF-BCA-987654"
        )

        val result = debitProcessor.processPayment(request)
        assertTrue(result is PaymentResult.Success)
        val success = result as PaymentResult.Success
        assertEquals("REF-BCA-987654", success.transactionReference)
    }

    // ========================================================================
    // 3. REGISTRY ROUTING TESTS
    // ========================================================================

    @Test
    fun `registry dispatches request to matching processor`() = runTest {
        val cashReq = PaymentRequest("ord-1", 20000.0, PaymentMethodType.CASH, 20000.0)
        val cashRes = registry.process(cashReq)
        assertTrue(cashRes is PaymentResult.Success)
        assertEquals(PaymentMethodType.CASH, (cashRes as PaymentResult.Success).method)

        val qrisReq = PaymentRequest("ord-2", 20000.0, PaymentMethodType.QRIS)
        val qrisRes = registry.process(qrisReq)
        assertTrue(qrisRes is PaymentResult.Success)
        assertEquals(PaymentMethodType.QRIS, (qrisRes as PaymentResult.Success).method)
    }

    // ========================================================================
    // 4. RECEIPT SPECIFICATION VERIFICATION (9 MANDATORY COMPONENTS)
    // ========================================================================

    @Test
    fun `receipt format contains all 9 required specifications`() {
        val receiptData = PrintReceiptData(
            outletName = "Dakota Coffee Shop",
            outletAddress = "Jl. Kopi Dakota No. 1, Jakarta",
            orderNumber = "DKT-20261014-001",
            dateTime = "14/10/2026 21:00",
            cashierName = "Rian Kasir",
            items = listOf(
                PrintReceiptItem(
                    name = "Es Kopi Susu Dakota",
                    variantModifier = "Large, Less Sugar",
                    quantity = 2,
                    unitPrice = 22000.0,
                    subtotal = 44000.0
                )
            ),
            subtotal = 44000.0,
            discount = 4000.0,
            tax = 0.0,
            total = 40000.0,
            paymentMethod = "CASH",
            cashReceived = 50000.0,
            change = 10000.0,
            referenceNumber = "CASH-101",
            footerMessage = "Terima kasih atas kunjungan Anda!",
            footerSocial = "Instagram: @dakotacoffee.id"
        )

        val formatted = ReceiptTextFormatter.formatReceipt(receiptData)

        // 1. Nama Outlet
        assertTrue("Outlet name must be present", formatted.contains("DAKOTA COFFEE SHOP"))
        // 2. Order Number
        assertTrue("Order number must be present", formatted.contains("DKT-20261014-001"))
        // 3. Tanggal & Waktu
        assertTrue("Date/Time must be present", formatted.contains("14/10/2026 21:00"))
        // 4. Nama Kasir
        assertTrue("Cashier name must be present", formatted.contains("Rian Kasir"))
        // 5. Detail Item: Produk, Varian, Qty, Harga satuan, Subtotal
        assertTrue("Item name must be present", formatted.contains("Es Kopi Susu Dakota"))
        assertTrue("Variant/modifier must be present", formatted.contains("Large, Less Sugar"))
        assertTrue("Quantity must be present", formatted.contains("2 x"))
        // 6. Diskon
        assertTrue("Discount must be present", formatted.contains("Diskon"))
        // 7. Total Pembayaran
        assertTrue("Total must be present", formatted.contains("TOTAL"))
        // 8. Metode Pembayaran & Detail (Tunai, Kembalian)
        assertTrue("Payment method must be present", formatted.contains("CASH"))
        assertTrue("Cash received must be present", formatted.contains("Tunai Diterima"))
        assertTrue("Change must be present", formatted.contains("Kembalian"))
        // 9. Footer Struk
        assertTrue("Footer message must be present", formatted.contains("Terima kasih atas kunjungan Anda!"))
        assertTrue("Footer social media must be present", formatted.contains("@dakotacoffee.id"))
    }

    // ========================================================================
    // 5. ESC/POS BINARY GENERATOR TESTS
    // ========================================================================

    @Test
    fun `esc pos generator creates valid byte sequence with init and cut commands`() {
        val receiptData = PrintReceiptData(
            outletName = "Dakota Coffee Shop",
            orderNumber = "DKT-001",
            dateTime = "14/10/2026 21:00",
            cashierName = "Rian",
            items = listOf(
                PrintReceiptItem(
                    name = "Americano",
                    quantity = 1,
                    unitPrice = 18000.0,
                    subtotal = 18000.0
                )
            ),
            subtotal = 18000.0,
            total = 18000.0,
            paymentMethod = "QRIS"
        )

        val bytes = EscPosReceiptGenerator.generateBytes(receiptData)
        assertTrue(bytes.isNotEmpty())

        // Cek init ESC @ (0x1B, 0x40)
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals('@'.code.toByte(), bytes[1])

        // Cek perintah cut paper GS V 1 (0x1D, 0x56, 0x01)
        val cutIndex = bytes.size - 3
        assertEquals(0x1D.toByte(), bytes[cutIndex])
        assertEquals('V'.code.toByte(), bytes[cutIndex + 1])
        assertEquals(1.toByte(), bytes[cutIndex + 2])
    }

    // ========================================================================
    // 6. PROCESS PAYMENT USE CASE & ORDER STATE TRANSITION
    // ========================================================================

    @Test
    fun `process payment transitions order to PAID and prints receipt`() = runTest {
        var savedStatus: OrderStatus = OrderStatus.CONFIRMED
        var savedPaymentMethod: String? = null

        val mockOrder = Order(
            id = "ord-test-99",
            orderNumber = "DKT-20261014-099",
            cashierId = "u-01",
            cashierName = "Rian",
            status = OrderStatus.CONFIRMED,
            items = listOf(
                OrderItem(
                    id = "item-1",
                    orderId = "ord-test-99",
                    productId = "p-1",
                    productName = "Latte",
                    quantity = 1,
                    unitPrice = 24000.0,
                    subtotal = 24000.0
                )
            ),
            subtotal = 24000.0,
            discount = 0.0,
            tax = 0.0,
            total = 24000.0
        )

        val fakeOrderRepo = object : OrderRepository {
            override suspend fun createOrderFromCart(cart: Cart, cashierId: String, cashierName: String, initialStatus: OrderStatus): Order = mockOrder
            override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Boolean {
                savedStatus = status
                return true
            }
            override suspend fun markOrderAsPaid(orderId: String, paymentMethod: String): Boolean {
                savedStatus = OrderStatus.PAID
                savedPaymentMethod = paymentMethod
                return true
            }
            override suspend fun getOrderById(orderId: String): Order? = mockOrder.copy(status = savedStatus, paymentMethod = savedPaymentMethod)
            override fun getRecentOrders(): Flow<List<Order>> = flowOf(listOf(mockOrder))
        }

        val processPaymentUseCase = ProcessPaymentUseCase(registry, fakeOrderRepo)
        val printReceiptUseCase = PrintReceiptUseCase(mockPrinter)

        val paymentResult = processPaymentUseCase(
            orderId = "ord-test-99",
            method = PaymentMethodType.CASH,
            cashReceived = 50000.0
        )

        assertTrue(paymentResult is ProcessPaymentUseCase.Result.Success)
        val success = paymentResult as ProcessPaymentUseCase.Result.Success
        assertEquals(OrderStatus.PAID, success.order.status)
        assertEquals("CASH", success.order.paymentMethod)
        assertEquals(26000.0, success.paymentResult.change, 0.001)

        // Print receipt
        val printExecution = printReceiptUseCase(
            order = success.order,
            cashReceived = 50000.0,
            change = success.paymentResult.change,
            referenceNumber = success.paymentResult.transactionReference
        )

        assertTrue(printExecution.printResult is PrintResult.Success)
        assertEquals(mockOrder.orderNumber, mockPrinter.lastPrintedReceipt?.orderNumber)

        // Cannot double-pay already paid order
        val duplicateResult = processPaymentUseCase(
            orderId = "ord-test-99",
            method = PaymentMethodType.CASH,
            cashReceived = 50000.0
        )
        assertTrue(duplicateResult is ProcessPaymentUseCase.Result.Error)
        assertTrue((duplicateResult as ProcessPaymentUseCase.Result.Error).message.contains("sudah dibayar"))
    }
}
