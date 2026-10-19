package com.dakota.pos

import com.dakota.pos.core.hardware.printer.PrinterInterface
import com.dakota.pos.core.hardware.printer.PrinterManager
import com.dakota.pos.core.hardware.printer.PrinterType
import com.dakota.pos.core.hardware.printer.StationTicketGenerator
import com.dakota.pos.domain.model.Category
import com.dakota.pos.domain.model.Modifier
import com.dakota.pos.domain.model.Order
import com.dakota.pos.domain.model.OrderItem
import com.dakota.pos.domain.model.OrderStatus
import com.dakota.pos.domain.model.Product
import com.dakota.pos.domain.model.kds.KitchenOrder
import com.dakota.pos.domain.model.kds.KitchenOrderStatus
import com.dakota.pos.domain.model.kds.StationConfig
import com.dakota.pos.domain.model.kds.WorkStation
import com.dakota.pos.domain.repository.KdsRepository
import com.dakota.pos.domain.repository.ProductRepository
import com.dakota.pos.domain.usecase.kds.DispatchOrderToKitchenUseCase
import com.dakota.pos.domain.usecase.kds.GetKdsOrdersUseCase
import com.dakota.pos.domain.usecase.kds.ManageStationConfigUseCase
import com.dakota.pos.domain.usecase.kds.PrintStationTicketUseCase
import com.dakota.pos.domain.usecase.kds.UpdateKitchenOrderStatusUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Unit Tests for Phase 11: KDS + Barista Printer.
 *
 * Test coverage:
 * 1. Station Routing: Pure Beverage order routes to WorkStation.BAR
 * 2. Station Routing: Pure Food order routes to WorkStation.KITCHEN
 * 3. Order Splitting: Mixed order (Coffee + Food + Pastry) splits into 3 tickets
 * 4. State Machine: Full progression NEW -> ACCEPTED -> PREPARING -> READY -> SERVED
 * 5. State Machine: Invalid transition rejections
 * 6. Order Cancellation: Transition to CANCELLED and verify terminal state
 * 7. Urgency & Elapsed Timer calculation: Normal (<5m), Warning (5-10m), Critical (>10m)
 * 8. Station Ticket Generation: Plain text & ESC/POS byte array format validation
 * 9. Auto-Print Dispatch: Auto-print triggers ticket printing to station printer
 * 10. Station Config Management: Update printer type and target address
 */
class KdsEngineTest {

    private lateinit var fakeKdsRepo: FakeKdsRepository
    private lateinit var fakeProductRepo: FakeProductRepository
    private lateinit var printerManager: PrinterManager
    private lateinit var spyBarPrinter: SpyPrinter
    private lateinit var spyKitchenPrinter: SpyPrinter

    private lateinit var printStationTicketUseCase: PrintStationTicketUseCase
    private lateinit var dispatchOrderToKitchenUseCase: DispatchOrderToKitchenUseCase
    private lateinit var updateKitchenOrderStatusUseCase: UpdateKitchenOrderStatusUseCase
    private lateinit var getKdsOrdersUseCase: GetKdsOrdersUseCase
    private lateinit var manageStationConfigUseCase: ManageStationConfigUseCase

    @Before
    fun setup() {
        fakeKdsRepo = FakeKdsRepository()
        fakeProductRepo = FakeProductRepository()
        printerManager = PrinterManager()

        spyBarPrinter = SpyPrinter()
        spyKitchenPrinter = SpyPrinter()
        printerManager.setPrinterForStation(WorkStation.BAR, spyBarPrinter)
        printerManager.setPrinterForStation(WorkStation.KITCHEN, spyKitchenPrinter)

        printStationTicketUseCase = PrintStationTicketUseCase(printerManager)
        dispatchOrderToKitchenUseCase = DispatchOrderToKitchenUseCase(
            kdsRepository = fakeKdsRepo,
            productRepository = fakeProductRepo,
            printStationTicketUseCase = printStationTicketUseCase
        )
        updateKitchenOrderStatusUseCase = UpdateKitchenOrderStatusUseCase(fakeKdsRepo)
        getKdsOrdersUseCase = GetKdsOrdersUseCase(fakeKdsRepo)
        manageStationConfigUseCase = ManageStationConfigUseCase(fakeKdsRepo, printerManager)

        // Setup mock catalog products
        fakeProductRepo.products = listOf(
            Product(id = "prod_latte", categoryId = "cat_coffee", name = "Caffe Latte", basePrice = 28000.0),
            Product(id = "prod_cappuccino", categoryId = "cat_coffee", name = "Cappuccino", basePrice = 30000.0),
            Product(id = "prod_nasi_goreng", categoryId = "cat_food", name = "Nasi Goreng Special", basePrice = 35000.0),
            Product(id = "prod_french_fries", categoryId = "cat_snack", name = "French Fries", basePrice = 20000.0),
            Product(id = "prod_croissant", categoryId = "cat_pastry", name = "Butter Croissant", basePrice = 25000.0)
        )
    }

    // ========================================================================
    // 1. STATION ROUTING & ORDER SPLITTING
    // ========================================================================

    @Test
    fun testStationRoutingPureBeverageRoutesToBar() = runBlocking {
        val order = Order(
            id = "ord-001",
            orderNumber = "DKT-20261019-001",
            cashierId = "cashier-1",
            cashierName = "Budi Barista",
            status = OrderStatus.PAID,
            subtotal = 58000.0,
            total = 58000.0,
            items = listOf(
                OrderItem(
                    id = "item-1",
                    orderId = "ord-001",
                    productId = "prod_latte",
                    productName = "Caffe Latte",
                    quantity = 2,
                    unitPrice = 28000.0,
                    subtotal = 56000.0,
                    variantName = "Hot / Large",
                    selectedModifiersSummary = "Oat Milk, Less Sugar"
                )
            )
        )

        val tickets = dispatchOrderToKitchenUseCase(order)

        assertEquals(1, tickets.size)
        val ticket = tickets.first()
        assertEquals(WorkStation.BAR, ticket.station)
        assertEquals(KitchenOrderStatus.NEW, ticket.status)
        assertEquals(1, ticket.items.size)
        assertEquals("Caffe Latte", ticket.items.first().productName)
        assertEquals("Hot / Large", ticket.items.first().variantName)
        assertEquals(2, ticket.items.first().quantity)
        assertEquals(1, spyBarPrinter.printCount)
    }

    @Test
    fun testStationRoutingPureFoodRoutesToKitchen() = runBlocking {
        val order = Order(
            id = "ord-002",
            orderNumber = "DKT-20261019-002",
            cashierId = "cashier-1",
            cashierName = "Budi",
            status = OrderStatus.PAID,
            subtotal = 35000.0,
            total = 35000.0,
            items = listOf(
                OrderItem(
                    id = "item-2",
                    orderId = "ord-002",
                    productId = "prod_nasi_goreng",
                    productName = "Nasi Goreng Special",
                    quantity = 1,
                    unitPrice = 35000.0,
                    subtotal = 35000.0,
                    notes = "Pedas level 3, jangan pakai timun"
                )
            )
        )

        val tickets = dispatchOrderToKitchenUseCase(order)

        assertEquals(1, tickets.size)
        val ticket = tickets.first()
        assertEquals(WorkStation.KITCHEN, ticket.station)
        assertEquals("Pedas level 3, jangan pakai timun", ticket.items.first().notes)
        assertEquals(1, spyKitchenPrinter.printCount)
    }

    @Test
    fun testOrderSplittingMultiStation() = runBlocking {
        val order = Order(
            id = "ord-003",
            orderNumber = "DKT-20261019-003",
            cashierId = "cashier-2",
            cashierName = "Siti",
            status = OrderStatus.PAID,
            subtotal = 113000.0,
            total = 113000.0,
            customerNotes = "Meja nomor 5",
            items = listOf(
                OrderItem(id = "i1", orderId = "ord-003", productId = "prod_latte", productName = "Caffe Latte", quantity = 1, unitPrice = 28000.0, subtotal = 28000.0),
                OrderItem(id = "i2", orderId = "ord-003", productId = "prod_cappuccino", productName = "Cappuccino", quantity = 1, unitPrice = 30000.0, subtotal = 30000.0),
                OrderItem(id = "i3", orderId = "ord-003", productId = "prod_nasi_goreng", productName = "Nasi Goreng Special", quantity = 1, unitPrice = 35000.0, subtotal = 35000.0),
                OrderItem(id = "i4", orderId = "ord-003", productId = "prod_croissant", productName = "Butter Croissant", quantity = 1, unitPrice = 25000.0, subtotal = 25000.0)
            )
        )

        val tickets = dispatchOrderToKitchenUseCase(order)

        assertEquals(3, tickets.size)

        val barTicket = tickets.find { it.station == WorkStation.BAR }
        assertNotNull(barTicket)
        assertEquals(2, barTicket!!.items.size)
        assertEquals("Meja nomor 5", barTicket.customerNotes)

        val kitchenTicket = tickets.find { it.station == WorkStation.KITCHEN }
        assertNotNull(kitchenTicket)
        assertEquals(1, kitchenTicket!!.items.size)
        assertEquals("Nasi Goreng Special", kitchenTicket.items.first().productName)

        val dessertTicket = tickets.find { it.station == WorkStation.DESSERT }
        assertNotNull(dessertTicket)
        assertEquals(1, dessertTicket!!.items.size)
        assertEquals("Butter Croissant", dessertTicket.items.first().productName)
    }

    // ========================================================================
    // 2. STATE MACHINE PROGRESSION TESTS
    // ========================================================================

    @Test
    fun testStateMachineFullLinearProgression() = runBlocking {
        val kitchenOrder = KitchenOrder(
            id = "kord-100",
            orderId = "ord-100",
            orderNumber = "DKT-100",
            station = WorkStation.BAR,
            status = KitchenOrderStatus.NEW,
            cashierName = "Budi"
        )
        fakeKdsRepo.saveKitchenOrder(kitchenOrder)

        // 1. NEW -> ACCEPTED
        val r1 = updateKitchenOrderStatusUseCase.advanceStatus("kord-100")
        assertTrue(r1.isSuccess)
        assertEquals(KitchenOrderStatus.ACCEPTED, r1.getOrThrow())
        assertEquals(KitchenOrderStatus.ACCEPTED, fakeKdsRepo.getKitchenOrderById("kord-100")?.status)

        // 2. ACCEPTED -> PREPARING
        val r2 = updateKitchenOrderStatusUseCase.advanceStatus("kord-100")
        assertTrue(r2.isSuccess)
        assertEquals(KitchenOrderStatus.PREPARING, r2.getOrThrow())

        // 3. PREPARING -> READY
        val r3 = updateKitchenOrderStatusUseCase.advanceStatus("kord-100")
        assertTrue(r3.isSuccess)
        assertEquals(KitchenOrderStatus.READY, r3.getOrThrow())

        // 4. READY -> SERVED
        val r4 = updateKitchenOrderStatusUseCase.advanceStatus("kord-100")
        assertTrue(r4.isSuccess)
        assertEquals(KitchenOrderStatus.SERVED, r4.getOrThrow())

        // 5. SERVED -> cannot advance further
        val r5 = updateKitchenOrderStatusUseCase.advanceStatus("kord-100")
        assertTrue(r5.isFailure)
        assertTrue(r5.exceptionOrNull()?.message?.contains("sudah selesai disajikan") == true)
    }

    @Test
    fun testStateMachineDirectTransitionsAndValidation() = runBlocking {
        val order = KitchenOrder(
            id = "kord-101",
            orderId = "ord-101",
            orderNumber = "DKT-101",
            station = WorkStation.BAR,
            status = KitchenOrderStatus.NEW,
            cashierName = "Budi"
        )
        fakeKdsRepo.saveKitchenOrder(order)

        // NEW -> PREPARING directly is allowed (skip accepted)
        val validDirect = updateKitchenOrderStatusUseCase("kord-101", KitchenOrderStatus.PREPARING)
        assertTrue(validDirect.isSuccess)

        // PREPARING -> SERVED directly is NOT allowed (must go through READY)
        val invalidSkip = updateKitchenOrderStatusUseCase("kord-101", KitchenOrderStatus.SERVED)
        assertTrue(invalidSkip.isFailure)

        // PREPARING -> CANCELLED is allowed
        val cancelResult = updateKitchenOrderStatusUseCase("kord-101", KitchenOrderStatus.CANCELLED)
        assertTrue(cancelResult.isSuccess)

        // CANCELLED -> ACCEPTED is rejected
        val resurrectResult = updateKitchenOrderStatusUseCase("kord-101", KitchenOrderStatus.ACCEPTED)
        assertTrue(resurrectResult.isFailure)
    }

    // ========================================================================
    // 3. TIMING & URGENCY TESTS
    // ========================================================================

    @Test
    fun testElapsedTimingAndUrgencyLevels() {
        val now = System.currentTimeMillis()

        // 1. Fresh order (2 minutes ago) -> Normal Green
        val freshOrder = KitchenOrder(
            id = "o1", orderId = "ord1", orderNumber = "001",
            station = WorkStation.BAR, cashierName = "Budi",
            orderTime = now - (2 * 60 * 1000L)
        )
        assertFalse(freshOrder.isWarning)
        assertFalse(freshOrder.isCritical)

        // 2. Warning order (7 minutes ago) -> Orange Warning
        val warningOrder = KitchenOrder(
            id = "o2", orderId = "ord2", orderNumber = "002",
            station = WorkStation.BAR, cashierName = "Budi",
            orderTime = now - (7 * 60 * 1000L)
        )
        assertTrue(warningOrder.isWarning)
        assertFalse(warningOrder.isCritical)

        // 3. Critical order (12 minutes ago) -> Red Critical
        val criticalOrder = KitchenOrder(
            id = "o3", orderId = "ord3", orderNumber = "003",
            station = WorkStation.BAR, cashierName = "Budi",
            orderTime = now - (12 * 60 * 1000L)
        )
        assertFalse(criticalOrder.isWarning)
        assertTrue(criticalOrder.isCritical)

        // 4. Served order -> Urgency cleared
        val servedOrder = criticalOrder.copy(status = KitchenOrderStatus.SERVED)
        assertFalse(servedOrder.isWarning)
        assertFalse(servedOrder.isCritical)
    }

    // ========================================================================
    // 4. TICKET GENERATOR & PRINTING
    // ========================================================================

    @Test
    fun testTicketGeneratorPlainTextOutput() {
        val order = KitchenOrder(
            id = "kord-200",
            orderId = "ord-200",
            orderNumber = "DKT-20261019-200",
            station = WorkStation.BAR,
            cashierName = "Budi",
            customerNotes = "Jangan pakai sedotan",
            items = listOf(
                com.dakota.pos.domain.model.kds.KitchenOrderItem(
                    id = "ki-1",
                    kitchenOrderId = "kord-200",
                    orderItemId = "oi-1",
                    productId = "prod_latte",
                    productName = "Caffe Latte",
                    quantity = 2,
                    variantName = "Hot",
                    modifiersSummary = "Oat Milk",
                    notes = "Extra hot"
                )
            )
        )

        val plainText = StationTicketGenerator.generatePlainText(order)

        assertTrue(plainText.contains("BARISTA BAR"))
        assertTrue(plainText.contains("Order #DKT-20261019-200"))
        assertTrue(plainText.contains("2x Caffe Latte"))
        assertTrue(plainText.contains("Varian: Hot"))
        assertTrue(plainText.contains("Mod: Oat Milk"))
        assertTrue(plainText.contains("NOTE: Extra hot"))
        assertTrue(plainText.contains("Jangan pakai sedotan"))
    }

    @Test
    fun testTicketGeneratorEscPosBytes() {
        val order = KitchenOrder(
            id = "kord-201",
            orderId = "ord-201",
            orderNumber = "DKT-20261019-201",
            station = WorkStation.KITCHEN,
            cashierName = "Siti",
            items = listOf(
                com.dakota.pos.domain.model.kds.KitchenOrderItem(
                    id = "ki-2",
                    kitchenOrderId = "kord-201",
                    orderItemId = "oi-2",
                    productId = "prod_nasi_goreng",
                    productName = "Nasi Goreng Special",
                    quantity = 1
                )
            )
        )

        val bytes = StationTicketGenerator.generateTicketBytes(order)

        assertTrue(bytes.isNotEmpty())
        // Should contain Init command [0x1B, '@']
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals('@'.code.toByte(), bytes[1])
        // Should end with Cut paper command [0x1D, 'V', 1]
        val len = bytes.size
        assertEquals(0x1D.toByte(), bytes[len - 3])
        assertEquals('V'.code.toByte(), bytes[len - 2])
        assertEquals(1.toByte(), bytes[len - 1])
    }

    @Test
    fun testStationConfigManagement() = runBlocking {
        manageStationConfigUseCase.initDefaults()

        val configs = fakeKdsRepo.getAllStationConfigs()
        assertEquals(3, configs.size)

        val barConfig = fakeKdsRepo.getStationConfig(WorkStation.BAR)
        assertNotNull(barConfig)

        // Update bar config to Network LAN
        val updatedConfig = barConfig!!.copy(
            printerType = PrinterType.NETWORK_LAN_ESC_POS,
            targetAddress = "TCP:192.168.1.150:9100",
            autoPrintOnDispatch = false
        )
        manageStationConfigUseCase.saveConfig(updatedConfig)

        val refreshed = fakeKdsRepo.getStationConfig(WorkStation.BAR)
        assertEquals(PrinterType.NETWORK_LAN_ESC_POS, refreshed?.printerType)
        assertEquals("TCP:192.168.1.150:9100", refreshed?.targetAddress)
        assertEquals(false, refreshed?.autoPrintOnDispatch)
    }

    // ========================================================================
    // TEST DOUBLES (FAKES & SPIES)
    // ========================================================================

    private class SpyPrinter : PrinterInterface {
        override val printerType: PrinterType = PrinterType.MOCK
        var isConn = true
        var printCount = 0
        var cutCount = 0
        val printedData = mutableListOf<ByteArray>()

        override suspend fun connect(targetAddress: String): Result<Boolean> {
            isConn = true
            return Result.success(true)
        }

        override suspend fun disconnect() {
            isConn = false
        }

        override suspend fun checkStatus(): com.dakota.pos.core.hardware.printer.PrinterStatus {
            return if (isConn) com.dakota.pos.core.hardware.printer.PrinterStatus.CONNECTED else com.dakota.pos.core.hardware.printer.PrinterStatus.DISCONNECTED
        }

        override suspend fun printReceipt(receipt: com.dakota.pos.core.hardware.printer.PrintReceiptData): com.dakota.pos.core.hardware.printer.PrintResult {
            return com.dakota.pos.core.hardware.printer.PrintResult.Success()
        }

        override suspend fun printBaristaTicket(
            orderNumber: String,
            stationName: String,
            items: List<com.dakota.pos.core.hardware.printer.PrintReceiptItem>
        ): com.dakota.pos.core.hardware.printer.PrintResult {
            return com.dakota.pos.core.hardware.printer.PrintResult.Success()
        }

        override suspend fun printRawBytes(data: ByteArray): Boolean {
            printCount++
            printedData.add(data)
            return true
        }

        override suspend fun cutPaper(): Boolean {
            cutCount++
            return true
        }

        override suspend fun openCashDrawer(): Boolean = true
    }

    private class FakeKdsRepository : KdsRepository {
        val orders = mutableMapOf<String, KitchenOrder>()
        val configs = mutableMapOf<WorkStation, StationConfig>()

        override suspend fun saveKitchenOrder(order: KitchenOrder) {
            orders[order.id] = order
        }

        override suspend fun getKitchenOrderById(id: String): KitchenOrder? = orders[id]

        override suspend fun updateOrderStatus(id: String, status: KitchenOrderStatus) {
            orders[id]?.let {
                val now = System.currentTimeMillis()
                orders[id] = it.copy(
                    status = status,
                    acceptedTime = if (status == KitchenOrderStatus.ACCEPTED) now else it.acceptedTime,
                    readyTime = if (status == KitchenOrderStatus.READY) now else it.readyTime,
                    servedTime = if (status == KitchenOrderStatus.SERVED) now else it.servedTime
                )
            }
        }

        override fun getActiveOrdersFlow(): Flow<List<KitchenOrder>> {
            val active = orders.values.filter { it.status != KitchenOrderStatus.SERVED && it.status != KitchenOrderStatus.CANCELLED }
            return flowOf(active.toList())
        }

        override fun getActiveOrdersByStationFlow(station: WorkStation): Flow<List<KitchenOrder>> {
            val active = orders.values.filter {
                it.station == station && it.status != KitchenOrderStatus.SERVED && it.status != KitchenOrderStatus.CANCELLED
            }
            return flowOf(active.toList())
        }

        override fun getAllOrdersFlow(): Flow<List<KitchenOrder>> = flowOf(orders.values.toList())

        override suspend fun getAllStationConfigs(): List<StationConfig> = configs.values.toList()

        override fun getStationConfigsFlow(): Flow<List<StationConfig>> = flowOf(configs.values.toList())

        override suspend fun getStationConfig(station: WorkStation): StationConfig? = configs[station]

        override suspend fun saveStationConfig(config: StationConfig) {
            configs[config.station] = config
        }

        override suspend fun seedDefaultStationConfigsIfEmpty() {
            if (configs.isEmpty()) {
                configs[WorkStation.BAR] = StationConfig(
                    station = WorkStation.BAR,
                    name = "Barista Bar",
                    printerType = PrinterType.MOCK,
                    targetAddress = "MOCK",
                    autoPrintOnDispatch = true,
                    categoryIds = listOf("cat_coffee", "cat_non_coffee", "cat_tea", "cat_signature", "cat_beverage")
                )
                configs[WorkStation.KITCHEN] = StationConfig(
                    station = WorkStation.KITCHEN,
                    name = "Kitchen Dapur",
                    printerType = PrinterType.MOCK,
                    targetAddress = "MOCK",
                    autoPrintOnDispatch = true,
                    categoryIds = listOf("cat_food", "cat_snack", "cat_main", "cat_side")
                )
                configs[WorkStation.DESSERT] = StationConfig(
                    station = WorkStation.DESSERT,
                    name = "Dessert & Pastry",
                    printerType = PrinterType.MOCK,
                    targetAddress = "MOCK",
                    autoPrintOnDispatch = true,
                    categoryIds = listOf("cat_pastry", "cat_dessert", "cat_bakery")
                )
            }
        }
    }

    private class FakeProductRepository : ProductRepository {
        var products = listOf<Product>()

        override fun getCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun getProducts(): Flow<List<Product>> = flowOf(products)
        override fun getProductsByCategory(categoryId: String): Flow<List<Product>> =
            flowOf(products.filter { it.categoryId == categoryId })
        override suspend fun getProductById(productId: String): Product? =
            products.find { it.id == productId }
        override fun getModifiers(): Flow<List<Modifier>> = flowOf(emptyList())
        override suspend fun seedInitialCatalogIfEmpty() {}
    }
}
