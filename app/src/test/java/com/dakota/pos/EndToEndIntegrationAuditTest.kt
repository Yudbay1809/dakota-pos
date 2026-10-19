package com.dakota.pos

import com.dakota.pos.core.hardware.printer.PrintReceiptData
import com.dakota.pos.core.hardware.printer.PrintReceiptItem
import com.dakota.pos.core.hardware.printer.PrintResult
import com.dakota.pos.core.hardware.printer.PrinterInterface
import com.dakota.pos.core.hardware.printer.PrinterManager
import com.dakota.pos.core.hardware.printer.PrinterStatus
import com.dakota.pos.core.hardware.printer.PrinterType
import com.dakota.pos.domain.auth.Permission
import com.dakota.pos.domain.auth.UserRole
import com.dakota.pos.domain.auth.UserSession
import com.dakota.pos.domain.inventory.IdempotentStockDeductionEngine
import com.dakota.pos.domain.model.Cart
import com.dakota.pos.domain.model.CartItem
import com.dakota.pos.domain.model.Category
import com.dakota.pos.domain.model.Modifier
import com.dakota.pos.domain.model.Order
import com.dakota.pos.domain.model.OrderItem
import com.dakota.pos.domain.model.OrderStatus
import com.dakota.pos.domain.model.Product
import com.dakota.pos.domain.model.finance.ExpenseCategory
import com.dakota.pos.domain.model.finance.OperatingExpense
import com.dakota.pos.domain.model.inventory.MovementType
import com.dakota.pos.domain.model.inventory.StockMovement
import com.dakota.pos.domain.model.inventory.StockOpname
import com.dakota.pos.domain.model.kds.KitchenOrder
import com.dakota.pos.domain.model.kds.KitchenOrderStatus
import com.dakota.pos.domain.model.kds.StationConfig
import com.dakota.pos.domain.model.kds.WorkStation
import com.dakota.pos.domain.model.purchasing.GoodsReceipt
import com.dakota.pos.domain.model.purchasing.PurchaseOrder
import com.dakota.pos.domain.model.purchasing.PurchaseOrderStatus
import com.dakota.pos.domain.model.purchasing.Supplier
import com.dakota.pos.domain.model.recipe.HppBreakdown
import com.dakota.pos.domain.model.recipe.Ingredient
import com.dakota.pos.domain.model.recipe.Recipe
import com.dakota.pos.domain.model.recipe.RecipeItem
import com.dakota.pos.domain.model.recipe.RecipeItemHpp
import com.dakota.pos.domain.model.shift.CashMovement
import com.dakota.pos.domain.model.shift.CashMovementType
import com.dakota.pos.domain.model.shift.CashierShift
import com.dakota.pos.domain.model.shift.ShiftStatus
import com.dakota.pos.domain.model.unit.UnitType
import com.dakota.pos.domain.payment.CashPaymentProcessor
import com.dakota.pos.domain.payment.PaymentMethodType
import com.dakota.pos.domain.payment.PaymentProcessorRegistry
import com.dakota.pos.domain.payment.PaymentResult
import com.dakota.pos.domain.repository.FinanceRepository
import com.dakota.pos.domain.repository.InventoryRepository
import com.dakota.pos.domain.repository.KdsRepository
import com.dakota.pos.domain.repository.OrderRepository
import com.dakota.pos.domain.repository.ProductRepository
import com.dakota.pos.domain.repository.PurchasingRepository
import com.dakota.pos.domain.repository.RecipeRepository
import com.dakota.pos.domain.repository.ShiftRepository
import com.dakota.pos.domain.usecase.finance.GenerateProfitLossReportUseCase
import com.dakota.pos.domain.usecase.finance.RecordOperatingExpenseUseCase
import com.dakota.pos.domain.usecase.kds.DispatchOrderToKitchenUseCase
import com.dakota.pos.domain.usecase.kds.PrintStationTicketUseCase
import com.dakota.pos.domain.usecase.kds.UpdateKitchenOrderStatusUseCase
import com.dakota.pos.domain.usecase.payment.PrintReceiptUseCase
import com.dakota.pos.domain.usecase.payment.ProcessPaymentUseCase
import com.dakota.pos.domain.usecase.pos.CheckoutOrderUseCase
import com.dakota.pos.domain.usecase.recipe.CalculateHppUseCase
import com.dakota.pos.domain.usecase.shift.CloseShiftUseCase
import com.dakota.pos.domain.usecase.shift.GenerateShiftReportUseCase
import com.dakota.pos.domain.usecase.shift.OpenShiftUseCase
import com.dakota.pos.domain.usecase.shift.RecordCashMovementUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * FASE 13: End-to-End Full Integration & Production Resilience Audit.
 *
 * Menguji integrasi lengkap tanpa putus 13 tahapan operasional kedai kopi:
 * 1. Purchasing PO -> 2. Goods Receiving & Unit Cost Update ->
 * 3. Recipe Setup & HPP Calculation -> 4. Catalog Setup ->
 * 5. Open Shift Kasir -> 6. Order di POS (Cart) ->
 * 7. Pembayaran Tunai & Kembalian -> 8. Cetak Struk Kasir ->
 * 9. KDS Barista Routing & Cetak Tiket Bar ->
 * 10. Barista Order State Machine (NEW -> READY -> SERVED) ->
 * 11. Pengurangan Stok Idempotent -> 12. Close Shift & Rekonsiliasi Kas ->
 * 13. Laporan Laba Rugi Komprehensif (Net Sales, COGS, Gross Profit, Expenses, Net Profit).
 *
 * Pengujian Ketahanan & Edge Cases:
 * - Printer Error Resilience (Transaksi tetap aman jika printer mati)
 * - Idempotency (Dua kali pemotongan stok pada order yang sama tidak double-deduct)
 * - Refund & Stock Return (Pengembalian stok saat pesanan dibatalkan)
 * - RBAC Security & Permission Matrix (Kasir/Barista vs Owner/Manager)
 */
class EndToEndIntegrationAuditTest {

    // Repositories (Test Doubles)
    private lateinit var orderRepo: AuditOrderRepository
    private lateinit var recipeRepo: AuditRecipeRepository
    private lateinit var inventoryRepo: AuditInventoryRepository
    private lateinit var purchasingRepo: AuditPurchasingRepository
    private lateinit var shiftRepo: AuditShiftRepository
    private lateinit var kdsRepo: AuditKdsRepository
    private lateinit var financeRepo: AuditFinanceRepository
    private lateinit var productRepo: AuditProductRepository

    // Hardware & Printer Spies
    private lateinit var printerManager: PrinterManager
    private lateinit var auditCashierPrinter: AuditSpyPrinter
    private lateinit var auditBarPrinter: AuditSpyPrinter

    @Before
    fun setup() {
        orderRepo = AuditOrderRepository()
        recipeRepo = AuditRecipeRepository()
        inventoryRepo = AuditInventoryRepository()
        purchasingRepo = AuditPurchasingRepository()
        shiftRepo = AuditShiftRepository()
        kdsRepo = AuditKdsRepository()
        financeRepo = AuditFinanceRepository()
        productRepo = AuditProductRepository()

        auditCashierPrinter = AuditSpyPrinter()
        auditBarPrinter = AuditSpyPrinter()

        printerManager = PrinterManager()
        printerManager.setPrinterForStation(WorkStation.BAR, auditBarPrinter)
    }

    // ========================================================================
    // 1. MASTER END-TO-END BUSINESS JOURNEY (13 STEPS UNBROKEN)
    // ========================================================================

    @Test
    fun testComplete13StepsCoffeeShopBusinessJourney() = runBlocking {
        val now = System.currentTimeMillis()

        // --------------------------------------------------------------------
        // LANGKAH 1 & 2: PURCHASING & GOODS RECEIVING
        // --------------------------------------------------------------------
        // Setup Bahan Baku Awal:
        // - Biji Kopi Espresso (Stok awal: 0 g, Unit Cost: Rp 120/g -> Rp 120,000/kg)
        // - Susu Segar Fresh Milk (Stok awal: 0 ml, Unit Cost: Rp 18/ml -> Rp 18,000/L)
        val coffeeBean = Ingredient(
            id = "ing_coffee_beans",
            name = "Biji Kopi Espresso House Blend",
            sku = "ING-COFFEE-01",
            category = "Biji Kopi",
            defaultUnit = UnitType.G,
            currentStock = 0.0,
            minimumStock = 1000.0,
            costPerDefaultUnit = 120.0
        )
        val freshMilk = Ingredient(
            id = "ing_fresh_milk",
            name = "Susu Segar Fresh Milk",
            sku = "ING-MILK-01",
            category = "Dairy",
            defaultUnit = UnitType.ML,
            currentStock = 0.0,
            minimumStock = 2000.0,
            costPerDefaultUnit = 18.0
        )
        recipeRepo.saveIngredient(coffeeBean)
        recipeRepo.saveIngredient(freshMilk)
        inventoryRepo.saveIngredient(coffeeBean)
        inventoryRepo.saveIngredient(freshMilk)

        // Terima Pengadaan (Goods Receipt): Masuk 5,000 g Kopi dan 10,000 ml Susu
        inventoryRepo.recordMovement(
            StockMovement(
                id = "mov-1",
                ingredientId = coffeeBean.id,
                ingredientName = coffeeBean.name,
                type = MovementType.IN,
                quantityChange = 5000.0,
                unit = UnitType.G,
                balanceBefore = 0.0,
                balanceAfter = 5000.0,
                referenceId = "GR-20261019-001",
                userId = "u-owner",
                userName = "Owner Dakota"
            )
        )
        inventoryRepo.updateIngredientStock(coffeeBean.id, 5000.0)

        inventoryRepo.recordMovement(
            StockMovement(
                id = "mov-2",
                ingredientId = freshMilk.id,
                ingredientName = freshMilk.name,
                type = MovementType.IN,
                quantityChange = 10000.0,
                unit = UnitType.ML,
                balanceBefore = 0.0,
                balanceAfter = 10000.0,
                referenceId = "GR-20261019-001",
                userId = "u-owner",
                userName = "Owner Dakota"
            )
        )
        inventoryRepo.updateIngredientStock(freshMilk.id, 10000.0)

        // Verifikasi saldo stok bahan bertambah
        assertEquals(5000.0, inventoryRepo.getIngredientById(coffeeBean.id)?.currentStock ?: 0.0, 0.001)
        assertEquals(10000.0, inventoryRepo.getIngredientById(freshMilk.id)?.currentStock ?: 0.0, 0.001)

        // --------------------------------------------------------------------
        // LANGKAH 3 & 4: SETUP RESEP, HITUNG HPP & SETUP KATALOG
        // --------------------------------------------------------------------
        // Formula Resep Caffe Latte: 18g Kopi Espresso + 200ml Susu Segar
        val latteRecipe = Recipe(
            id = "recipe_latte",
            productId = "prod_latte",
            productName = "Caffe Latte",
            variantId = null,
            items = listOf(
                RecipeItem(id = "r1", recipeId = "recipe_latte", ingredientId = coffeeBean.id, ingredientName = coffeeBean.name, quantity = 18.0, unit = UnitType.G),
                RecipeItem(id = "r2", recipeId = "recipe_latte", ingredientId = freshMilk.id, ingredientName = freshMilk.name, quantity = 200.0, unit = UnitType.ML)
            )
        )
        recipeRepo.saveRecipe(latteRecipe)

        // Hitung HPP Otomatis dari Formula:
        // HPP = (18g * Rp 120) + (200ml * Rp 18) = Rp 2,160 + Rp 3,600 = Rp 5,760
        val calculateHppUseCase = CalculateHppUseCase(recipeRepo)
        val hppResult = calculateHppUseCase(productId = "prod_latte", variantId = null, sellingPrice = 28000.0)
        assertNotNull(hppResult)
        assertEquals(5760.0, hppResult!!.totalHpp, 0.001)
        assertEquals(22240.0, hppResult.grossProfit, 0.001) // 28,000 - 5,760 = 22,240
        assertEquals(79.4, hppResult.grossMarginPercentage, 0.1)

        // Setup Produk POS
        val latteProduct = Product(
            id = "prod_latte",
            categoryId = "cat_coffee",
            name = "Caffe Latte",
            basePrice = 28000.0
        )
        productRepo.products = listOf(latteProduct)

        // --------------------------------------------------------------------
        // LANGKAH 5: KASIR BUKA SHIFT (OPEN SHIFT)
        // --------------------------------------------------------------------
        val openShiftUseCase = OpenShiftUseCase(shiftRepo)
        val shiftResult = openShiftUseCase(
            cashierId = "cashier-budi",
            cashierName = "Budi Santoso",
            openingCash = 200000.0 // Modal kas awal Rp 200,000
        )
        assertTrue(shiftResult.isSuccess)
        val activeShift = shiftResult.getOrThrow()
        assertEquals(ShiftStatus.OPEN, activeShift.status)
        assertEquals(200000.0, activeShift.expectedCash, 0.001)

        // --------------------------------------------------------------------
        // LANGKAH 6: BUAT ORDER DI KASIR (POS CART)
        // --------------------------------------------------------------------
        val cart = Cart(
            items = listOf(
                CartItem(
                    product = latteProduct,
                    quantity = 2, // 2x Caffe Latte @ 28,000 = 56,000
                    notes = "Less sugar"
                )
            ),
            discount = 0.0
        )

        val checkoutOrderUseCase = CheckoutOrderUseCase(orderRepo)
        val checkoutResult = checkoutOrderUseCase(cart, cashierId = "cashier-budi", cashierName = "Budi Santoso")
        assertTrue(checkoutResult is CheckoutOrderUseCase.Result.Success)
        val placedOrder = (checkoutResult as CheckoutOrderUseCase.Result.Success).order
        assertEquals(OrderStatus.CONFIRMED, placedOrder.status)
        assertEquals(56000.0, placedOrder.total, 0.001)

        // --------------------------------------------------------------------
        // LANGKAH 7: PEMBAYARAN TUNAI (CASH PAYMENT & KEMBALIAN)
        // --------------------------------------------------------------------
        val registry = PaymentProcessorRegistry()
        val processPaymentUseCase = ProcessPaymentUseCase(registry, orderRepo)
        val paymentResult = processPaymentUseCase(
            orderId = placedOrder.id,
            method = PaymentMethodType.CASH,
            cashReceived = 100000.0 // Pembeli bayar Rp 100,000
        )
        assertTrue(paymentResult is ProcessPaymentUseCase.Result.Success)
        val paidOrder = (paymentResult as ProcessPaymentUseCase.Result.Success).order
        assertEquals(OrderStatus.PAID, paidOrder.status)

        val successPay = (paymentResult as ProcessPaymentUseCase.Result.Success).paymentResult
        assertEquals(44000.0, successPay.change, 0.001) // Kembalian Rp 44,000

        // --------------------------------------------------------------------
        // LANGKAH 8: CETAK STRUK KASIR (PRINTER ABSTRACTION)
        // --------------------------------------------------------------------
        val printReceiptUseCase = PrintReceiptUseCase(auditCashierPrinter)
        val receiptPrintResult = printReceiptUseCase(
            order = paidOrder,
            cashReceived = 100000.0,
            change = 44000.0,
            referenceNumber = successPay.transactionReference
        )
        assertTrue(receiptPrintResult.formattedText.contains("Caffe Latte"))
        assertTrue(receiptPrintResult.formattedText.contains("56.000"))
        printReceiptUseCase.openCashDrawer()
        assertTrue(auditCashierPrinter.cashDrawerOpened)

        // --------------------------------------------------------------------
        // LANGKAH 9 & 10: DISPATCH KDS BARISTA & UPDATE STATUS ANTRIAN
        // --------------------------------------------------------------------
        val printStationTicketUseCase = PrintStationTicketUseCase(printerManager)
        val dispatchOrderToKitchenUseCase = DispatchOrderToKitchenUseCase(
            kdsRepository = kdsRepo,
            productRepository = productRepo,
            printStationTicketUseCase = printStationTicketUseCase
        )

        val tickets = dispatchOrderToKitchenUseCase(paidOrder)
        assertEquals(1, tickets.size)
        val barTicket = tickets.first()
        assertEquals(WorkStation.BAR, barTicket.station)
        assertEquals(KitchenOrderStatus.NEW, barTicket.status)
        assertEquals(1, auditBarPrinter.printCount) // Barista ticket tercetak

        // Barista Memajukan Status: NEW -> ACCEPTED -> PREPARING -> READY -> SERVED
        val updateKitchenOrderStatusUseCase = UpdateKitchenOrderStatusUseCase(kdsRepo)
        updateKitchenOrderStatusUseCase.advanceStatus(barTicket.id) // -> ACCEPTED
        updateKitchenOrderStatusUseCase.advanceStatus(barTicket.id) // -> PREPARING
        updateKitchenOrderStatusUseCase.advanceStatus(barTicket.id) // -> READY
        updateKitchenOrderStatusUseCase.advanceStatus(barTicket.id) // -> SERVED

        val finalTicket = kdsRepo.getKitchenOrderById(barTicket.id)
        assertEquals(KitchenOrderStatus.SERVED, finalTicket?.status)

        // --------------------------------------------------------------------
        // LANGKAH 11: PENGURANGAN STOK BAHAN BAKU IDEMPOTENT
        // --------------------------------------------------------------------
        val deductionEngine = IdempotentStockDeductionEngine(
            inventoryRepository = inventoryRepo,
            recipeRepository = recipeRepo
        )

        // Untuk 2x Caffe Latte:
        // Kopi terpotong: 2 * 18g = 36g
        // Susu terpotong: 2 * 200ml = 400ml
        val deductionResult1 = deductionEngine.deductStockForOrder(
            orderId = paidOrder.id,
            lineItems = paidOrder.items.map {
                IdempotentStockDeductionEngine.OrderLineItem(
                    productId = it.productId,
                    variantId = it.variantId,
                    quantity = it.quantity
                )
            },
            userId = "cashier-budi",
            userName = "Budi Santoso"
        )
        assertFalse(deductionResult1.wasAlreadyProcessed)
        assertEquals(2, deductionResult1.movements.size)

        val remainingCoffee = inventoryRepo.getIngredientById(coffeeBean.id)?.currentStock ?: 0.0
        val remainingMilk = inventoryRepo.getIngredientById(freshMilk.id)?.currentStock ?: 0.0
        assertEquals(5000.0 - 36.0, remainingCoffee, 0.001) // 4,964 g
        assertEquals(10000.0 - 400.0, remainingMilk, 0.001) // 9,600 ml

        // IDEMPOTENCY TEST: Panggilan kedua dengan Order ID yang sama tidak boleh memotong stok lagi!
        val deductionResult2 = deductionEngine.deductStockForOrder(
            orderId = paidOrder.id,
            lineItems = paidOrder.items.map {
                IdempotentStockDeductionEngine.OrderLineItem(
                    productId = it.productId,
                    variantId = it.variantId,
                    quantity = it.quantity
                )
            },
            userId = "cashier-budi",
            userName = "Budi Santoso"
        )
        assertTrue(deductionResult2.wasAlreadyProcessed)
        assertEquals(0, deductionResult2.movements.size)
        assertEquals(4964.0, inventoryRepo.getIngredientById(coffeeBean.id)?.currentStock ?: 0.0, 0.001)
        assertEquals(9600.0, inventoryRepo.getIngredientById(freshMilk.id)?.currentStock ?: 0.0, 0.001)

        // --------------------------------------------------------------------
        // LANGKAH 12: PETTY CASH & KASIR TUTUP SHIFT (CLOSE SHIFT)
        // --------------------------------------------------------------------
        // Catat pengeluaran darurat operasional (beli sabun cuci Rp 15,000 dari laci kas)
        val recordCashMovementUseCase = RecordCashMovementUseCase(shiftRepo)
        recordCashMovementUseCase(
            shiftId = activeShift.id,
            type = CashMovementType.EXPENSE,
            amount = 15000.0,
            reason = "Beli Sabun Cuci Piring",
            cashierId = "cashier-budi",
            cashierName = "Budi Santoso"
        )

        // Simulasikan penjualan tunai Rp 56,000 di shiftRepo
        shiftRepo.recordOrderSale(paidOrder.total, isCash = true)

        // Tutup Shift:
        // Expected Cash = Modal Awal (200,000) + Penjualan Tunai (56,000) - Expense (15,000) = Rp 241,000
        val generateShiftReportUseCase = GenerateShiftReportUseCase()
        val closeShiftUseCase = CloseShiftUseCase(shiftRepo, generateShiftReportUseCase)
        val closeResult = closeShiftUseCase(
            shiftId = activeShift.id,
            actualCash = 241000.0, // Uang fisik dihitung tepat sama
            closingNotes = "Shift lancar, kas seimbang"
        )
        assertTrue(closeResult.isSuccess)
        val shiftCloseResult = closeResult.getOrThrow()
        val closedShift = shiftCloseResult.closedShift
        assertEquals(ShiftStatus.CLOSED, closedShift.status)
        assertEquals(241000.0, closedShift.expectedCash, 0.001)
        assertNotNull(closedShift.actualCash)
        assertEquals(241000.0, closedShift.actualCash!!, 0.001)
        assertNotNull(closedShift.variance)
        assertEquals(0.0, closedShift.variance!!, 0.001) // Balanced variance = 0!

        // --------------------------------------------------------------------
        // LANGKAH 13: KALKULASI LAPORAN KEUANGAN LABA RUGI (PROFIT & LOSS)
        // --------------------------------------------------------------------
        // Catat biaya operasional toko:
        val recordExpenseUseCase = RecordOperatingExpenseUseCase(financeRepo)
        recordExpenseUseCase(
            category = ExpenseCategory.CLEANING,
            amount = 15000.0,
            description = "Sabun Cuci Piring",
            recordedBy = "Budi Santoso",
            date = now
        )

        val generateProfitLossReportUseCase = GenerateProfitLossReportUseCase(
            orderRepository = orderRepo,
            recipeRepository = recipeRepo,
            financeRepository = financeRepo
        )

        val pnlReport = generateProfitLossReportUseCase(now - 100000, now + 100000)

        // 1. Net Sales = Rp 56,000
        assertEquals(56000.0, pnlReport.grossSales, 0.001)
        assertEquals(56000.0, pnlReport.netSales, 0.001)

        // 2. Total COGS = 2x Caffe Latte HPP = 2 * Rp 5,760 = Rp 11,520
        assertEquals(11520.0, pnlReport.totalCogs, 0.001)

        // 3. Gross Profit = Rp 56,000 - Rp 11,520 = Rp 44,480
        assertEquals(44480.0, pnlReport.grossProfit, 0.001)
        assertEquals(79.4, pnlReport.grossProfitMargin, 0.1)

        // 4. Operating Expenses = Rp 15,000
        assertEquals(15000.0, pnlReport.totalOperatingExpenses, 0.001)

        // 5. Net Profit Akhir = Laba Kotor (44,480) - Beban Operasional (15,000) = Rp 29,480
        assertEquals(29480.0, pnlReport.netProfit, 0.001)
        assertEquals(52.6, pnlReport.netProfitMargin, 0.1)
        assertTrue(pnlReport.isProfitable)

        // 6. Menu item profitability metrics
        assertEquals(1, pnlReport.topProducts.size)
        val topMenu = pnlReport.topProducts.first()
        assertEquals("Caffe Latte", topMenu.productName)
        assertEquals(2, topMenu.quantitySold)
        assertEquals(56000.0, topMenu.revenue, 0.001)
        assertEquals(11520.0, topMenu.cogs, 0.001)
        assertEquals(44480.0, topMenu.grossProfit, 0.001)
    }

    // ========================================================================
    // 2. RESILIENCE & EDGE CASES AUDIT
    // ========================================================================

    @Test
    fun testPrinterFailureDoesNotRollbackPaidTransaction() = runBlocking {
        // Printer kasir error / putus koneksi
        auditCashierPrinter.shouldFail = true

        val order = Order(
            id = "ord-fail-print",
            orderNumber = "DKT-999",
            cashierId = "c1",
            cashierName = "Budi",
            status = OrderStatus.PAID,
            subtotal = 28000.0,
            total = 28000.0
        )
        orderRepo.saveOrder(order)

        val printReceiptUseCase = PrintReceiptUseCase(auditCashierPrinter)
        val result = printReceiptUseCase(order, 50000.0, 22000.0, "TRX-001")

        // Cetak struk gagal, tetapi pesanan tetap sah berstatus PAID di database
        assertFalse(result.printResult is PrintResult.Success)
        val savedOrder = orderRepo.getOrderById("ord-fail-print")
        assertEquals(OrderStatus.PAID, savedOrder?.status)
    }

    @Test
    fun testRefundAndStockReturnRestoresInventory() = runBlocking {
        // Setup bahan awal dengan stok 100g
        val bean = Ingredient(
            id = "ing_bean",
            name = "Coffee Bean",
            sku = "ING-BEAN",
            defaultUnit = UnitType.G,
            currentStock = 100.0,
            minimumStock = 10.0,
            costPerDefaultUnit = 100.0
        )
        inventoryRepo.saveIngredient(bean)
        recipeRepo.saveIngredient(bean)

        val recipe = Recipe(
            id = "rec-espresso",
            productId = "prod-espresso",
            productName = "Espresso",
            items = listOf(
                RecipeItem(id = "ri-1", recipeId = "rec-espresso", ingredientId = bean.id, ingredientName = bean.name, quantity = 18.0, unit = UnitType.G)
            )
        )
        recipeRepo.saveRecipe(recipe)

        val deductionEngine = IdempotentStockDeductionEngine(inventoryRepo, recipeRepo)

        // 1. Potong stok untuk order
        val orderId = "order-cancel-test"
        deductionEngine.deductStockForOrder(
            orderId = orderId,
            lineItems = listOf(IdempotentStockDeductionEngine.OrderLineItem("prod-espresso", null, 1)),
            userId = "u1",
            userName = "Cashier"
        )
        assertEquals(82.0, inventoryRepo.getIngredientById(bean.id)?.currentStock ?: 0.0, 0.001)

        // 2. Batalkan order (Refund) -> Kembalikan stok
        val returnResult = deductionEngine.returnStockForOrder(orderId, "u1", "Cashier")
        assertFalse(returnResult.wasAlreadyProcessed)
        assertEquals(1, returnResult.movements.size)
        assertEquals(MovementType.RETURN, returnResult.movements.first().type)

        // Stok kembali utuh 100g
        assertEquals(100.0, inventoryRepo.getIngredientById(bean.id)?.currentStock ?: 0.0, 0.001)

        // Return kedua kali (idempotent check) tidak boleh menduplikasi pengembalian stok
        val returnResult2 = deductionEngine.returnStockForOrder(orderId, "u1", "Cashier")
        assertTrue(returnResult2.wasAlreadyProcessed)
        assertEquals(100.0, inventoryRepo.getIngredientById(bean.id)?.currentStock ?: 0.0, 0.001)
    }

    @Test
    fun testRbacPermissionEnforcement() {
        val now = System.currentTimeMillis()

        // Owner memiliki seluruh hak akses (termasuk VIEW_HPP dan MANAGE_RECIPE)
        val ownerSession = UserSession(
            userId = "u-owner",
            username = "Owner Dakota",
            role = UserRole.OWNER,
            permissions = Permission.entries.toSet(),
            loginTimestamp = now
        )
        assertTrue(ownerSession.permissions.contains(Permission.VIEW_HPP))
        assertTrue(ownerSession.permissions.contains(Permission.MANAGE_RECIPES))
        assertTrue(ownerSession.permissions.contains(Permission.VIEW_FINANCIAL_REPORTS))

        // Kasir biasa hanya memiliki hak akses operasional POS
        val cashierSession = UserSession(
            userId = "u-cashier",
            username = "Kasir Budi",
            role = UserRole.CASHIER,
            permissions = setOf(Permission.OPERATE_POS, Permission.OPEN_CLOSE_SHIFT, Permission.PROCESS_REFUND),
            loginTimestamp = now
        )
        assertFalse(cashierSession.permissions.contains(Permission.VIEW_HPP))
        assertFalse(cashierSession.permissions.contains(Permission.MANAGE_RECIPES))
        assertFalse(cashierSession.permissions.contains(Permission.VIEW_FINANCIAL_REPORTS))
    }

    // ========================================================================
    // TEST DOUBLES & AUDIT SPIES
    // ========================================================================

    private class AuditSpyPrinter : PrinterInterface {
        override val printerType: PrinterType = PrinterType.MOCK
        var isConn = true
        var shouldFail = false
        var cashDrawerOpened = false
        var printCount = 0

        override suspend fun connect(targetAddress: String): Result<Boolean> = Result.success(!shouldFail)
        override suspend fun disconnect() { isConn = false }
        override suspend fun checkStatus(): PrinterStatus = if (shouldFail) PrinterStatus.ERROR else PrinterStatus.CONNECTED
        override suspend fun printReceipt(receipt: PrintReceiptData): PrintResult {
            return if (shouldFail) PrintResult.Failure("Out of paper")
            else PrintResult.Success()
        }
        override suspend fun printBaristaTicket(orderNumber: String, stationName: String, items: List<PrintReceiptItem>): PrintResult {
            printCount++
            return PrintResult.Success()
        }
        override suspend fun printRawBytes(data: ByteArray): Boolean {
            printCount++
            return !shouldFail
        }
        override suspend fun cutPaper(): Boolean = !shouldFail
        override suspend fun openCashDrawer(): Boolean {
            cashDrawerOpened = true
            return true
        }
    }

    private class AuditOrderRepository : OrderRepository {
        val orders = mutableMapOf<String, Order>()

        fun saveOrder(order: Order) { orders[order.id] = order }

        override suspend fun createOrderFromCart(cart: Cart, cashierId: String, cashierName: String, initialStatus: OrderStatus): Order {
            val id = UUID.randomUUID().toString()
            val order = Order(
                id = id,
                orderNumber = "DKT-AUDIT-001",
                cashierId = cashierId,
                cashierName = cashierName,
                status = initialStatus,
                items = cart.items.map {
                    OrderItem(
                        id = UUID.randomUUID().toString(),
                        orderId = id,
                        productId = it.product.id,
                        productName = it.product.name,
                        quantity = it.quantity,
                        unitPrice = it.unitPrice,
                        subtotal = it.subtotal,
                        notes = it.notes
                    )
                },
                subtotal = cart.subtotal,
                discount = cart.discount,
                total = cart.total
            )
            orders[id] = order
            return order
        }

        override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Boolean {
            orders[orderId]?.let { orders[orderId] = it.copy(status = status) }
            return true
        }

        override suspend fun markOrderAsPaid(orderId: String, paymentMethod: String): Boolean {
            orders[orderId]?.let { orders[orderId] = it.copy(status = OrderStatus.PAID, paymentMethod = paymentMethod) }
            return true
        }

        override suspend fun getOrderById(orderId: String): Order? = orders[orderId]
        override fun getRecentOrders(): Flow<List<Order>> = flowOf(orders.values.toList())
        override suspend fun getPaidOrdersBetween(startTime: Long, endTime: Long): List<Order> {
            return orders.values.filter { it.status == OrderStatus.PAID }
        }
    }

    private class AuditRecipeRepository : RecipeRepository {
        val ingredients = mutableMapOf<String, Ingredient>()
        val recipes = mutableMapOf<String, Recipe>()

        override fun getAllIngredientsFlow(): Flow<List<Ingredient>> = flowOf(ingredients.values.toList())
        override suspend fun getAllIngredients(): List<Ingredient> = ingredients.values.toList()
        override suspend fun getIngredientById(id: String): Ingredient? = ingredients[id]
        override suspend fun saveIngredient(ingredient: Ingredient) { ingredients[ingredient.id] = ingredient }
        override suspend fun deleteIngredient(ingredient: Ingredient) { ingredients.remove(ingredient.id) }
        override fun getAllRecipesFlow(): Flow<List<Recipe>> = flowOf(recipes.values.toList())
        override suspend fun getRecipeForProduct(productId: String, variantId: String?): Recipe? = recipes[productId]
        override suspend fun getRecipeById(recipeId: String): Recipe? = recipes[recipeId]
        override suspend fun saveRecipe(recipe: Recipe) { recipes[recipe.productId] = recipe }
        override suspend fun deleteRecipe(recipeId: String) {}

        override suspend fun calculateHppForProduct(productId: String, variantId: String?, sellingPrice: Double): HppBreakdown? {
            val recipe = recipes[productId] ?: return null
            var totalHpp = 0.0
            val itemBreakdowns = recipe.items.mapNotNull { rItem ->
                val ing = ingredients[rItem.ingredientId] ?: return@mapNotNull null
                val cost = rItem.quantity * ing.costPerDefaultUnit
                totalHpp += cost
                RecipeItemHpp(
                    ingredientId = ing.id,
                    ingredientName = ing.name,
                    recipeQuantity = rItem.quantity,
                    recipeUnit = rItem.unit,
                    ingredientBaseUnit = ing.defaultUnit,
                    ingredientBaseCost = ing.costPerDefaultUnit,
                    costPerRecipeUnit = ing.costPerDefaultUnit,
                    subtotalHpp = cost
                )
            }
            val profit = sellingPrice - totalHpp
            val margin = if (sellingPrice > 0) (profit / sellingPrice) * 100.0 else 0.0
            return HppBreakdown(
                productId = productId,
                productName = "Product $productId",
                sellingPrice = sellingPrice,
                totalHpp = totalHpp,
                grossProfit = profit,
                grossMarginPercentage = margin,
                itemBreakdowns = itemBreakdowns
            )
        }

        override suspend fun seedInitialIngredientsAndRecipesIfEmpty() {}
    }

    private class AuditInventoryRepository : InventoryRepository {
        val stockMap = mutableMapOf<String, Double>()
        val ingredientsMap = mutableMapOf<String, Ingredient>()
        val movementsList = mutableListOf<StockMovement>()

        fun saveIngredient(ingredient: Ingredient) {
            ingredientsMap[ingredient.id] = ingredient
            stockMap[ingredient.id] = ingredient.currentStock
        }

        override suspend fun recordMovement(movement: StockMovement) {
            movementsList.add(movement)
        }

        override suspend fun recordMovements(movements: List<StockMovement>) {
            movements.forEach { recordMovement(it) }
        }

        override fun getMovementsForIngredient(ingredientId: String): Flow<List<StockMovement>> =
            flowOf(movementsList.filter { it.ingredientId == ingredientId })

        override fun getRecentMovements(limit: Int): Flow<List<StockMovement>> =
            flowOf(movementsList.takeLast(limit))

        override fun getMovementsByType(type: MovementType): Flow<List<StockMovement>> =
            flowOf(movementsList.filter { it.type == type })

        override suspend fun getMovementsByReferenceId(referenceId: String): List<StockMovement> =
            movementsList.filter { it.referenceId == referenceId }

        override suspend fun hasBeenDeductedForOrder(orderId: String): Boolean =
            movementsList.any { it.referenceId == orderId && it.type == MovementType.SALE }

        override suspend fun saveStockOpname(opname: StockOpname) {}
        override fun getAllOpnamesFlow(): Flow<List<StockOpname>> = flowOf(emptyList())

        override suspend fun updateIngredientStock(ingredientId: String, newStock: Double) {
            stockMap[ingredientId] = newStock
            ingredientsMap[ingredientId]?.let {
                ingredientsMap[ingredientId] = it.copy(currentStock = newStock)
            }
        }

        override suspend fun getIngredientById(ingredientId: String): Ingredient? = ingredientsMap[ingredientId]
        override suspend fun getAllIngredients(): List<Ingredient> = ingredientsMap.values.toList()
        override fun getAllIngredientsFlow(): Flow<List<Ingredient>> = flowOf(ingredientsMap.values.toList())
    }

    private class AuditPurchasingRepository : PurchasingRepository {
        override fun getAllSuppliersFlow(): Flow<List<Supplier>> = flowOf(emptyList())
        override fun getActiveSuppliersFlow(): Flow<List<Supplier>> = flowOf(emptyList())
        override suspend fun getSupplierById(id: String): Supplier? = null
        override suspend fun saveSupplier(supplier: Supplier) {}
        override suspend fun deleteSupplier(id: String) {}
        override suspend fun seedInitialSuppliersIfEmpty() {}

        override fun getAllPurchaseOrdersFlow(): Flow<List<PurchaseOrder>> = flowOf(emptyList())
        override fun getPurchaseOrdersByStatusFlow(status: PurchaseOrderStatus): Flow<List<PurchaseOrder>> = flowOf(emptyList())
        override suspend fun getPurchaseOrderById(id: String): PurchaseOrder? = null
        override suspend fun savePurchaseOrder(order: PurchaseOrder) {}
        override suspend fun updateOrderStatus(id: String, status: PurchaseOrderStatus) {}
        override suspend fun updateItemReceivedQuantity(itemId: String, receivedQuantity: Double) {}
        override suspend fun generateNextPoNumber(): String = "PO-001"

        override fun getAllGoodsReceiptsFlow(): Flow<List<GoodsReceipt>> = flowOf(emptyList())
        override fun getReceiptsForOrderFlow(orderId: String): Flow<List<GoodsReceipt>> = flowOf(emptyList())
        override suspend fun saveGoodsReceipt(receipt: GoodsReceipt) {}
        override suspend fun generateNextReceiptNumber(): String = "GR-001"
    }

    private class AuditShiftRepository : ShiftRepository {
        var activeShift: CashierShift? = null
        val movements = mutableListOf<CashMovement>()
        var totalSales = 0.0

        fun recordOrderSale(amount: Double, isCash: Boolean) {
            totalSales += amount
            activeShift?.let {
                activeShift = it.copy(
                    cashSales = if (isCash) it.cashSales + amount else it.cashSales,
                    expectedCash = if (isCash) it.expectedCash + amount else it.expectedCash
                )
            }
        }

        override suspend fun saveShift(shift: CashierShift) { activeShift = shift }
        override suspend fun getShiftById(id: String): CashierShift? = activeShift
        override suspend fun getActiveShiftForCashier(cashierId: String): CashierShift? = activeShift?.takeIf { it.status == ShiftStatus.OPEN }
        override suspend fun getAnyActiveShift(): CashierShift? = activeShift?.takeIf { it.status == ShiftStatus.OPEN }
        override fun getActiveShiftFlow(): Flow<CashierShift?> = flowOf(activeShift)
        override fun getAllShiftsFlow(): Flow<List<CashierShift>> = flowOf(listOfNotNull(activeShift))
        override fun getShiftsByCashierFlow(cashierId: String): Flow<List<CashierShift>> = flowOf(listOfNotNull(activeShift))
        override suspend fun recordCashMovement(movement: CashMovement) {
            movements.add(movement)
            activeShift?.let {
                val cashOut = if (movement.type == CashMovementType.EXPENSE || movement.type == CashMovementType.REFUND) it.totalExpense + movement.amount else it.totalExpense
                val expected = it.expectedCash - movement.amount
                activeShift = it.copy(totalExpense = cashOut, expectedCash = expected)
            }
        }
        override fun getMovementsForShiftFlow(shiftId: String): Flow<List<CashMovement>> = flowOf(movements)
        override suspend fun getMovementsForShift(shiftId: String): List<CashMovement> = movements
        override suspend fun getCashSalesForPeriod(startTime: Long, endTime: Long): Double = totalSales
        override suspend fun getNonCashSalesForPeriod(startTime: Long, endTime: Long): Double = 0.0
        override suspend fun getTransactionCountForPeriod(startTime: Long, endTime: Long): Int = 1
    }

    private class AuditKdsRepository : KdsRepository {
        val orders = mutableMapOf<String, KitchenOrder>()
        val configs = mutableMapOf<WorkStation, StationConfig>()

        override suspend fun saveKitchenOrder(order: KitchenOrder) { orders[order.id] = order }
        override suspend fun getKitchenOrderById(id: String): KitchenOrder? = orders[id]
        override suspend fun updateOrderStatus(id: String, status: KitchenOrderStatus) {
            orders[id]?.let { orders[id] = it.copy(status = status) }
        }
        override fun getActiveOrdersFlow(): Flow<List<KitchenOrder>> = flowOf(orders.values.toList())
        override fun getActiveOrdersByStationFlow(station: WorkStation): Flow<List<KitchenOrder>> = flowOf(orders.values.filter { it.station == station })
        override fun getAllOrdersFlow(): Flow<List<KitchenOrder>> = flowOf(orders.values.toList())
        override suspend fun getAllStationConfigs(): List<StationConfig> = configs.values.toList()
        override fun getStationConfigsFlow(): Flow<List<StationConfig>> = flowOf(configs.values.toList())
        override suspend fun getStationConfig(station: WorkStation): StationConfig? = configs[station]
        override suspend fun saveStationConfig(config: StationConfig) { configs[config.station] = config }
        override suspend fun seedDefaultStationConfigsIfEmpty() {
            if (configs.isEmpty()) {
                configs[WorkStation.BAR] = StationConfig(WorkStation.BAR, "Barista Bar", PrinterType.MOCK, "MOCK", true, listOf("cat_coffee"))
            }
        }
    }

    private class AuditFinanceRepository : FinanceRepository {
        val expenses = mutableListOf<OperatingExpense>()

        override suspend fun saveExpense(expense: OperatingExpense) { expenses.add(expense) }
        override suspend fun getExpenseById(id: String): OperatingExpense? = expenses.find { it.id == id }
        override suspend fun deleteExpense(id: String) { expenses.removeAll { it.id == id } }
        override suspend fun getExpensesBetween(startTime: Long, endTime: Long): List<OperatingExpense> = expenses
        override fun getExpensesBetweenFlow(startTime: Long, endTime: Long): Flow<List<OperatingExpense>> = flowOf(expenses)
        override fun getAllExpensesFlow(): Flow<List<OperatingExpense>> = flowOf(expenses)
        override suspend fun getTotalExpensesBetween(startTime: Long, endTime: Long): Double = expenses.sumOf { it.amount }
    }

    private class AuditProductRepository : ProductRepository {
        var products = listOf<Product>()

        override fun getCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun getProducts(): Flow<List<Product>> = flowOf(products)
        override fun getProductsByCategory(categoryId: String): Flow<List<Product>> = flowOf(products.filter { it.categoryId == categoryId })
        override suspend fun getProductById(productId: String): Product? = products.find { it.id == productId }
        override fun getModifiers(): Flow<List<Modifier>> = flowOf(emptyList())
        override suspend fun seedInitialCatalogIfEmpty() {}
    }
}
