package com.dakota.pos

import com.dakota.pos.domain.model.inventory.MovementType
import com.dakota.pos.domain.model.inventory.StockMovement
import com.dakota.pos.domain.model.inventory.StockOpname
import com.dakota.pos.domain.model.purchasing.GoodsReceipt
import com.dakota.pos.domain.model.purchasing.PurchaseOrder
import com.dakota.pos.domain.model.purchasing.PurchaseOrderItem
import com.dakota.pos.domain.model.purchasing.PurchaseOrderStatus
import com.dakota.pos.domain.model.purchasing.Supplier
import com.dakota.pos.domain.model.recipe.HppBreakdown
import com.dakota.pos.domain.model.recipe.Ingredient
import com.dakota.pos.domain.model.recipe.Recipe
import com.dakota.pos.domain.model.unit.UnitType
import com.dakota.pos.domain.repository.InventoryRepository
import com.dakota.pos.domain.repository.PurchasingRepository
import com.dakota.pos.domain.repository.RecipeRepository
import com.dakota.pos.domain.usecase.purchasing.CreatePOUseCase
import com.dakota.pos.domain.usecase.purchasing.IncomingItemInput
import com.dakota.pos.domain.usecase.purchasing.ReceiveGoodsUseCase
import com.dakota.pos.domain.usecase.purchasing.UpdateIngredientCostUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Comprehensive Unit Tests for Phase 09: Purchasing + Supplier.
 *
 * Test coverage:
 * 1. PO Creation as DRAFT with valid items and calculations
 * 2. PO Creation directly as SUBMITTED
 * 3. Validation: Empty items rejection
 * 4. Validation: Invalid quantity or unit price rejection
 * 5. Validation: Non-existent supplier rejection
 * 6. Goods Receiving: Full delivery sets PO to COMPLETED, updates stock & HPP, writes IN movement
 * 7. Goods Receiving: Partial delivery sets PO to RECEIVED, tracks remaining quantities correctly
 * 8. Goods Receiving: Multi-stage partial receiving culminating in COMPLETED status
 * 9. Validation: Cannot receive goods on DRAFT PO
 * 10. Validation: Cannot receive goods on COMPLETED PO
 * 11. Validation: Cannot receive goods on CANCELLED PO
 * 12. Moving Average Cost: Standard calculation (old stock + incoming stock)
 * 13. Moving Average Cost: Zero initial stock takes new purchase price
 * 14. Moving Average Cost: Unit conversion (e.g. ordered in KG, ingredient base unit in G)
 * 15. Audit Trail: StockMovement contains correct referenceId (receiptNumber) and type IN
 * 16. PO Status transition: DRAFT to SUBMITTED
 */
class PurchasingEngineTest {

    private lateinit var fakePurchasingRepo: FakePurchasingRepository
    private lateinit var fakeInventoryRepo: FakePurchasingInventoryRepository
    private lateinit var fakeRecipeRepo: FakePurchasingRecipeRepository

    private lateinit var createPOUseCase: CreatePOUseCase
    private lateinit var updateIngredientCostUseCase: UpdateIngredientCostUseCase
    private lateinit var receiveGoodsUseCase: ReceiveGoodsUseCase

    @Before
    fun setup() {
        fakePurchasingRepo = FakePurchasingRepository()
        fakeInventoryRepo = FakePurchasingInventoryRepository()
        fakeRecipeRepo = FakePurchasingRecipeRepository()

        updateIngredientCostUseCase = UpdateIngredientCostUseCase(fakeRecipeRepo)
        createPOUseCase = CreatePOUseCase(fakePurchasingRepo)
        receiveGoodsUseCase = ReceiveGoodsUseCase(
            purchasingRepository = fakePurchasingRepo,
            inventoryRepository = fakeInventoryRepo,
            recipeRepository = fakeRecipeRepo,
            updateIngredientCostUseCase = updateIngredientCostUseCase
        )

        // Setup test supplier
        val supplier = Supplier(
            id = "sup-kopi-01",
            name = "PT Sukses Kopi Nusantara",
            phone = "08123456789",
            paymentTerms = "NET_14"
        )
        fakePurchasingRepo.suppliersMap[supplier.id] = supplier

        // Setup test ingredients
        val coffeeBeans = Ingredient(
            id = "ing-kopi",
            name = "Biji Kopi House Blend",
            sku = "BB-001",
            defaultUnit = UnitType.G,
            costPerDefaultUnit = 200.0, // Rp 200/g = Rp 200.000/kg
            currentStock = 1000.0 // 1.000 gram awal
        )
        val freshMilk = Ingredient(
            id = "ing-susu",
            name = "Susu Fresh Milk",
            sku = "BB-002",
            defaultUnit = UnitType.ML,
            costPerDefaultUnit = 20.0, // Rp 20/ml = Rp 20.000/L
            currentStock = 5000.0 // 5.000 ml awal
        )

        fakeRecipeRepo.ingredientsMap[coffeeBeans.id] = coffeeBeans
        fakeRecipeRepo.ingredientsMap[freshMilk.id] = freshMilk
        fakeInventoryRepo.ingredientsMap[coffeeBeans.id] = coffeeBeans
        fakeInventoryRepo.ingredientsMap[freshMilk.id] = freshMilk
    }

    // ========================================================================
    // 1. PO CREATION TESTS
    // ========================================================================

    @Test
    fun testCreatePODraftSuccess() = runBlocking {
        val items = listOf(
            PurchaseOrderItem(
                id = "",
                purchaseOrderId = "",
                ingredientId = "ing-kopi",
                ingredientName = "Biji Kopi House Blend",
                unit = UnitType.G,
                orderedQuantity = 2000.0,
                unitPrice = 220.0
            ),
            PurchaseOrderItem(
                id = "",
                purchaseOrderId = "",
                ingredientId = "ing-susu",
                ingredientName = "Susu Fresh Milk",
                unit = UnitType.ML,
                orderedQuantity = 5000.0,
                unitPrice = 22.0
            )
        )

        val result = createPOUseCase(
            supplierId = "sup-kopi-01",
            items = items,
            notes = "Pesanan mingguan",
            createdBy = "Admin",
            submitImmediately = false
        )

        assertTrue(result.isSuccess)
        val po = result.getOrThrow()
        assertEquals(PurchaseOrderStatus.DRAFT, po.status)
        assertEquals("PT Sukses Kopi Nusantara", po.supplierName)
        assertEquals(2, po.items.size)

        // Subtotal checks
        // item 1: 2000 * 220 = 440.000
        // item 2: 5000 * 22 = 110.000
        // Total = 550.000
        assertEquals(550000.0, po.totalAmount, 0.001)
        assertTrue(po.poNumber.startsWith("PO-"))
    }

    @Test
    fun testCreatePOSubmittedDirectly() = runBlocking {
        val items = listOf(
            PurchaseOrderItem(
                id = "",
                purchaseOrderId = "",
                ingredientId = "ing-kopi",
                ingredientName = "Biji Kopi House Blend",
                unit = UnitType.KG,
                orderedQuantity = 5.0,
                unitPrice = 220000.0
            )
        )

        val result = createPOUseCase(
            supplierId = "sup-kopi-01",
            items = items,
            createdBy = "Manager",
            submitImmediately = true
        )

        assertTrue(result.isSuccess)
        val po = result.getOrThrow()
        assertEquals(PurchaseOrderStatus.SUBMITTED, po.status)
        assertEquals(1100000.0, po.totalAmount, 0.001)
    }

    @Test
    fun testCreatePOValidationEmptyItems() = runBlocking {
        val result = createPOUseCase(
            supplierId = "sup-kopi-01",
            items = emptyList(),
            createdBy = "Admin"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("tidak boleh kosong") == true)
    }

    @Test
    fun testCreatePOValidationInvalidQuantityOrPrice() = runBlocking {
        val invalidQtyItems = listOf(
            PurchaseOrderItem(
                id = "",
                purchaseOrderId = "",
                ingredientId = "ing-kopi",
                ingredientName = "Biji Kopi House Blend",
                unit = UnitType.G,
                orderedQuantity = 0.0,
                unitPrice = 200.0
            )
        )
        val result1 = createPOUseCase(supplierId = "sup-kopi-01", items = invalidQtyItems, createdBy = "Admin")
        assertTrue(result1.isFailure)

        val invalidPriceItems = listOf(
            PurchaseOrderItem(
                id = "",
                purchaseOrderId = "",
                ingredientId = "ing-kopi",
                ingredientName = "Biji Kopi House Blend",
                unit = UnitType.G,
                orderedQuantity = 100.0,
                unitPrice = -50.0
            )
        )
        val result2 = createPOUseCase(supplierId = "sup-kopi-01", items = invalidPriceItems, createdBy = "Admin")
        assertTrue(result2.isFailure)
    }

    @Test
    fun testCreatePOValidationNonExistentSupplier() = runBlocking {
        val items = listOf(
            PurchaseOrderItem(
                id = "",
                purchaseOrderId = "",
                ingredientId = "ing-kopi",
                ingredientName = "Biji Kopi House Blend",
                unit = UnitType.G,
                orderedQuantity = 100.0,
                unitPrice = 200.0
            )
        )
        val result = createPOUseCase(supplierId = "sup-ghost", items = items, createdBy = "Admin")
        assertTrue(result.isFailure)
    }

    // ========================================================================
    // 2. GOODS RECEIVING TESTS (FULL, PARTIAL, LIFECYCLE)
    // ========================================================================

    @Test
    fun testReceiveGoodsFullDelivery() = runBlocking {
        // Create and submit a PO
        val items = listOf(
            PurchaseOrderItem(
                id = "item-kopi-1",
                purchaseOrderId = "",
                ingredientId = "ing-kopi",
                ingredientName = "Biji Kopi House Blend",
                unit = UnitType.G,
                orderedQuantity = 1000.0,
                unitPrice = 240.0
            )
        )
        val po = createPOUseCase(
            supplierId = "sup-kopi-01",
            items = items,
            createdBy = "Admin",
            submitImmediately = true
        ).getOrThrow()

        // Receive full quantity (1000g)
        val incoming = listOf(
            IncomingItemInput(purchaseOrderItemId = po.items[0].id, quantityReceived = 1000.0)
        )
        val receiveResult = receiveGoodsUseCase(
            purchaseOrderId = po.id,
            receivedBy = "Gudang Kasir",
            receivedItems = incoming,
            receiptNotes = "Surat Jalan SJ-001 Lengkap"
        )

        assertTrue(receiveResult.isSuccess)
        val receipt = receiveResult.getOrThrow()
        assertTrue(receipt.receiptNumber.startsWith("GR-"))
        assertEquals(1, receipt.items.size)
        assertEquals(1000.0, receipt.items[0].receivedQuantity, 0.001)

        // Check PO status is now COMPLETED
        val updatedPo = fakePurchasingRepo.getPurchaseOrderById(po.id)
        assertNotNull(updatedPo)
        assertEquals(PurchaseOrderStatus.COMPLETED, updatedPo!!.status)
        assertEquals(1000.0, updatedPo.items[0].receivedQuantity, 0.001)
        assertTrue(updatedPo.items[0].isFullyReceived)

        // Check stock updated: 1000 initial + 1000 received = 2000
        val updatedStock = fakeInventoryRepo.ingredientsMap["ing-kopi"]!!.currentStock
        assertEquals(2000.0, updatedStock, 0.001)

        // Check stock movement recorded: type = IN
        val movements = fakeInventoryRepo.movements
        assertEquals(1, movements.size)
        val movement = movements[0]
        assertEquals(MovementType.IN, movement.type)
        assertEquals(1000.0, movement.quantityChange, 0.001)
        assertEquals(1000.0, movement.balanceBefore, 0.001)
        assertEquals(2000.0, movement.balanceAfter, 0.001)
        assertEquals(receipt.receiptNumber, movement.referenceId)
    }

    @Test
    fun testReceiveGoodsPartialDelivery() = runBlocking {
        // Create and submit a PO with 2 items
        val items = listOf(
            PurchaseOrderItem(
                id = "item-kopi-part",
                purchaseOrderId = "",
                ingredientId = "ing-kopi",
                ingredientName = "Biji Kopi House Blend",
                unit = UnitType.G,
                orderedQuantity = 2000.0,
                unitPrice = 250.0
            ),
            PurchaseOrderItem(
                id = "item-susu-part",
                purchaseOrderId = "",
                ingredientId = "ing-susu",
                ingredientName = "Susu Fresh Milk",
                unit = UnitType.ML,
                orderedQuantity = 5000.0,
                unitPrice = 20.0
            )
        )
        val po = createPOUseCase(
            supplierId = "sup-kopi-01",
            items = items,
            createdBy = "Admin",
            submitImmediately = true
        ).getOrThrow()

        // Receive partial: 1000g kopi (out of 2000g), and 0 susu
        val incoming = listOf(
            IncomingItemInput(purchaseOrderItemId = po.items[0].id, quantityReceived = 1000.0)
        )
        val receiveResult = receiveGoodsUseCase(
            purchaseOrderId = po.id,
            receivedBy = "Gudang Kasir",
            receivedItems = incoming
        )

        assertTrue(receiveResult.isSuccess)

        // Status must be RECEIVED (partial), NOT COMPLETED
        val updatedPo = fakePurchasingRepo.getPurchaseOrderById(po.id)!!
        assertEquals(PurchaseOrderStatus.RECEIVED, updatedPo.status)

        val kopiItem = updatedPo.items.find { it.ingredientId == "ing-kopi" }!!
        assertEquals(1000.0, kopiItem.receivedQuantity, 0.001)
        assertEquals(1000.0, kopiItem.remainingQuantity, 0.001)
        assertFalse(kopiItem.isFullyReceived)
    }

    @Test
    fun testReceiveGoodsMultipleBatchesToCompletion() = runBlocking {
        val items = listOf(
            PurchaseOrderItem(
                id = "item-kopi-multi",
                purchaseOrderId = "",
                ingredientId = "ing-kopi",
                ingredientName = "Biji Kopi House Blend",
                unit = UnitType.G,
                orderedQuantity = 2000.0,
                unitPrice = 250.0
            )
        )
        val po = createPOUseCase(
            supplierId = "sup-kopi-01",
            items = items,
            createdBy = "Admin",
            submitImmediately = true
        ).getOrThrow()

        // Batch 1: receive 800g
        val batch1 = receiveGoodsUseCase(
            purchaseOrderId = po.id,
            receivedBy = "Gudang",
            receivedItems = listOf(IncomingItemInput(po.items[0].id, 800.0))
        )
        assertTrue(batch1.isSuccess)
        assertEquals(PurchaseOrderStatus.RECEIVED, fakePurchasingRepo.getPurchaseOrderById(po.id)!!.status)

        // Batch 2: receive remaining 1200g
        val batch2 = receiveGoodsUseCase(
            purchaseOrderId = po.id,
            receivedBy = "Gudang",
            receivedItems = listOf(IncomingItemInput(po.items[0].id, 1200.0))
        )
        assertTrue(batch2.isSuccess)

        // Should now be COMPLETED
        val finalPo = fakePurchasingRepo.getPurchaseOrderById(po.id)!!
        assertEquals(PurchaseOrderStatus.COMPLETED, finalPo.status)
        assertEquals(2000.0, finalPo.items[0].receivedQuantity, 0.001)
        assertEquals(0.0, finalPo.items[0].remainingQuantity, 0.001)
        assertTrue(finalPo.items[0].isFullyReceived)
    }

    @Test
    fun testReceiveGoodsOnDraftPOFails() = runBlocking {
        val items = listOf(
            PurchaseOrderItem(
                id = "item-draft",
                purchaseOrderId = "",
                ingredientId = "ing-kopi",
                ingredientName = "Biji Kopi House Blend",
                unit = UnitType.G,
                orderedQuantity = 1000.0,
                unitPrice = 200.0
            )
        )
        // Saved as DRAFT
        val po = createPOUseCase(
            supplierId = "sup-kopi-01",
            items = items,
            createdBy = "Admin",
            submitImmediately = false
        ).getOrThrow()

        val result = receiveGoodsUseCase(
            purchaseOrderId = po.id,
            receivedBy = "Kasir",
            receivedItems = listOf(IncomingItemInput(po.items[0].id, 1000.0))
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("masih berstatus DRAFT") == true)
    }

    @Test
    fun testReceiveGoodsOnCompletedPOFails() = runBlocking {
        val items = listOf(
            PurchaseOrderItem(
                id = "item-comp",
                purchaseOrderId = "",
                ingredientId = "ing-kopi",
                ingredientName = "Biji Kopi House Blend",
                unit = UnitType.G,
                orderedQuantity = 1000.0,
                unitPrice = 200.0
            )
        )
        val po = createPOUseCase(
            supplierId = "sup-kopi-01",
            items = items,
            createdBy = "Admin",
            submitImmediately = true
        ).getOrThrow()

        // Receive full
        receiveGoodsUseCase(
            purchaseOrderId = po.id,
            receivedBy = "Kasir",
            receivedItems = listOf(IncomingItemInput(po.items[0].id, 1000.0))
        )

        // Try receive again on COMPLETED PO
        val extraReceive = receiveGoodsUseCase(
            purchaseOrderId = po.id,
            receivedBy = "Kasir",
            receivedItems = listOf(IncomingItemInput(po.items[0].id, 500.0))
        )

        assertTrue(extraReceive.isFailure)
        assertTrue(extraReceive.exceptionOrNull()?.message?.contains("sudah selesai") == true)
    }

    @Test
    fun testReceiveGoodsOnCancelledPOFails() = runBlocking {
        val items = listOf(
            PurchaseOrderItem(
                id = "item-canc",
                purchaseOrderId = "",
                ingredientId = "ing-kopi",
                ingredientName = "Biji Kopi House Blend",
                unit = UnitType.G,
                orderedQuantity = 1000.0,
                unitPrice = 200.0
            )
        )
        val po = createPOUseCase(
            supplierId = "sup-kopi-01",
            items = items,
            createdBy = "Admin",
            submitImmediately = true
        ).getOrThrow()

        fakePurchasingRepo.updateOrderStatus(po.id, PurchaseOrderStatus.CANCELLED)

        val result = receiveGoodsUseCase(
            purchaseOrderId = po.id,
            receivedBy = "Kasir",
            receivedItems = listOf(IncomingItemInput(po.items[0].id, 1000.0))
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("telah dibatalkan") == true)
    }

    // ========================================================================
    // 3. MOVING AVERAGE COST CALCULATION TESTS
    // ========================================================================

    @Test
    fun testUpdateIngredientCostMovingAverageStandard() = runBlocking {
        // Initial stock: 1000g @ Rp 200/g = Rp 200.000 value
        // Incoming goods: 1000g @ Rp 300/g = Rp 300.000 value
        // Total value = 500.000, Total qty = 2000g
        // New Moving Average Cost = 500.000 / 2000 = Rp 250/g
        val result = updateIngredientCostUseCase(
            ingredientId = "ing-kopi",
            receivedQuantity = 1000.0,
            receivedUnit = UnitType.G,
            receivedUnitPrice = 300.0
        )

        assertTrue(result.isSuccess)
        val newCost = result.getOrThrow()
        assertEquals(250.0, newCost, 0.001)

        val savedIng = fakeRecipeRepo.getIngredientById("ing-kopi")!!
        assertEquals(250.0, savedIng.costPerDefaultUnit, 0.001)
    }

    @Test
    fun testUpdateIngredientCostMovingAverageInitialStockZero() = runBlocking {
        val zeroStockIng = Ingredient(
            id = "ing-sirup",
            name = "Sirup Karamel",
            sku = "BB-003",
            defaultUnit = UnitType.ML,
            costPerDefaultUnit = 0.0,
            currentStock = 0.0
        )
        fakeRecipeRepo.ingredientsMap[zeroStockIng.id] = zeroStockIng

        // Incoming: 1000ml @ Rp 75/ml
        val result = updateIngredientCostUseCase(
            ingredientId = "ing-sirup",
            receivedQuantity = 1000.0,
            receivedUnit = UnitType.ML,
            receivedUnitPrice = 75.0
        )

        assertTrue(result.isSuccess)
        assertEquals(75.0, result.getOrThrow(), 0.001)
    }

    @Test
    fun testUpdateIngredientCostMovingAverageUnitConversion() = runBlocking {
        // Initial stock: 1000g @ Rp 200/g (Value = 200.000)
        // Ordered in KG: 2 kg @ Rp 240.000 / kg -> in grams: 2000g @ Rp 240 / g (Value = 480.000)
        // Total value = 200.000 + 480.000 = 680.000
        // Total quantity in grams = 1000 + 2000 = 3000g
        // Expected Moving Average Cost = 680.000 / 3000 = 226.66667 / g
        val result = updateIngredientCostUseCase(
            ingredientId = "ing-kopi",
            receivedQuantity = 2.0,
            receivedUnit = UnitType.KG,
            receivedUnitPrice = 240000.0
        )

        assertTrue(result.isSuccess)
        val newCost = result.getOrThrow()
        assertEquals(226.6666, newCost, 0.01)
    }

    @Test
    fun testPOStatusTransitionDraftToSubmitted() = runBlocking {
        val items = listOf(
            PurchaseOrderItem(
                id = "item-trans",
                purchaseOrderId = "",
                ingredientId = "ing-kopi",
                ingredientName = "Biji Kopi",
                unit = UnitType.G,
                orderedQuantity = 500.0,
                unitPrice = 200.0
            )
        )
        val po = createPOUseCase(
            supplierId = "sup-kopi-01",
            items = items,
            createdBy = "Admin",
            submitImmediately = false
        ).getOrThrow()

        assertEquals(PurchaseOrderStatus.DRAFT, po.status)

        fakePurchasingRepo.updateOrderStatus(po.id, PurchaseOrderStatus.SUBMITTED)
        val updated = fakePurchasingRepo.getPurchaseOrderById(po.id)!!
        assertEquals(PurchaseOrderStatus.SUBMITTED, updated.status)
    }
}

// ============================================================================
// FAKES FOR TESTING
// ============================================================================

class FakePurchasingRepository : PurchasingRepository {
    val suppliersMap = mutableMapOf<String, Supplier>()
    val purchaseOrdersMap = mutableMapOf<String, PurchaseOrder>()
    val goodsReceiptsMap = mutableMapOf<String, GoodsReceipt>()
    private var poCounter = 1
    private var grCounter = 1

    override fun getAllSuppliersFlow(): Flow<List<Supplier>> = flowOf(suppliersMap.values.toList())
    override fun getActiveSuppliersFlow(): Flow<List<Supplier>> = flowOf(suppliersMap.values.filter { it.isActive })
    override suspend fun getSupplierById(id: String): Supplier? = suppliersMap[id]
    override suspend fun saveSupplier(supplier: Supplier) { suppliersMap[supplier.id] = supplier }
    override suspend fun deleteSupplier(id: String) { suppliersMap.remove(id) }
    override suspend fun seedInitialSuppliersIfEmpty() {}

    override fun getAllPurchaseOrdersFlow(): Flow<List<PurchaseOrder>> = flowOf(purchaseOrdersMap.values.toList())
    override fun getPurchaseOrdersByStatusFlow(status: PurchaseOrderStatus): Flow<List<PurchaseOrder>> =
        flowOf(purchaseOrdersMap.values.filter { it.status == status })

    override suspend fun getPurchaseOrderById(id: String): PurchaseOrder? = purchaseOrdersMap[id]
    override suspend fun savePurchaseOrder(order: PurchaseOrder) { purchaseOrdersMap[order.id] = order }

    override suspend fun updateOrderStatus(id: String, status: PurchaseOrderStatus) {
        purchaseOrdersMap[id]?.let {
            purchaseOrdersMap[id] = it.copy(status = status, updatedAt = System.currentTimeMillis())
        }
    }

    override suspend fun updateItemReceivedQuantity(itemId: String, receivedQuantity: Double) {
        for ((poId, po) in purchaseOrdersMap) {
            val idx = po.items.indexOfFirst { it.id == itemId }
            if (idx != -1) {
                val updatedItems = po.items.toMutableList()
                updatedItems[idx] = updatedItems[idx].copy(receivedQuantity = receivedQuantity)
                purchaseOrdersMap[poId] = po.copy(items = updatedItems)
                break
            }
        }
    }

    override suspend fun generateNextPoNumber(): String = "PO-TEST-%03d".format(poCounter++)

    override fun getAllGoodsReceiptsFlow(): Flow<List<GoodsReceipt>> = flowOf(goodsReceiptsMap.values.toList())
    override fun getReceiptsForOrderFlow(orderId: String): Flow<List<GoodsReceipt>> =
        flowOf(goodsReceiptsMap.values.filter { it.purchaseOrderId == orderId })

    override suspend fun saveGoodsReceipt(receipt: GoodsReceipt) { goodsReceiptsMap[receipt.id] = receipt }
    override suspend fun generateNextReceiptNumber(): String = "GR-TEST-%03d".format(grCounter++)
}

class FakePurchasingInventoryRepository : InventoryRepository {
    val ingredientsMap = mutableMapOf<String, Ingredient>()
    val movements = mutableListOf<StockMovement>()

    override suspend fun recordMovement(movement: StockMovement) { movements.add(movement) }
    override suspend fun recordMovements(movements: List<StockMovement>) { this.movements.addAll(movements) }
    override fun getMovementsForIngredient(ingredientId: String): Flow<List<StockMovement>> = flowOf(movements.filter { it.ingredientId == ingredientId })
    override fun getRecentMovements(limit: Int): Flow<List<StockMovement>> = flowOf(movements.takeLast(limit))
    override fun getMovementsByType(type: MovementType): Flow<List<StockMovement>> = flowOf(movements.filter { it.type == type })
    override suspend fun getMovementsByReferenceId(referenceId: String): List<StockMovement> = movements.filter { it.referenceId == referenceId }
    override suspend fun hasBeenDeductedForOrder(orderId: String): Boolean = false
    override suspend fun saveStockOpname(opname: StockOpname) {}
    override fun getAllOpnamesFlow(): Flow<List<StockOpname>> = flowOf(emptyList())

    override suspend fun updateIngredientStock(ingredientId: String, newStock: Double) {
        ingredientsMap[ingredientId]?.let {
            ingredientsMap[ingredientId] = it.copy(currentStock = newStock)
        }
    }
    override suspend fun getIngredientById(ingredientId: String): Ingredient? = ingredientsMap[ingredientId]
    override suspend fun getAllIngredients(): List<Ingredient> = ingredientsMap.values.toList()
    override fun getAllIngredientsFlow(): Flow<List<Ingredient>> = flowOf(ingredientsMap.values.toList())
}

class FakePurchasingRecipeRepository : RecipeRepository {
    val ingredientsMap = mutableMapOf<String, Ingredient>()

    override fun getAllIngredientsFlow(): Flow<List<Ingredient>> = flowOf(ingredientsMap.values.toList())
    override suspend fun getAllIngredients(): List<Ingredient> = ingredientsMap.values.toList()
    override suspend fun getIngredientById(id: String): Ingredient? = ingredientsMap[id]
    override suspend fun saveIngredient(ingredient: Ingredient) { ingredientsMap[ingredient.id] = ingredient }
    override suspend fun deleteIngredient(ingredient: Ingredient) { ingredientsMap.remove(ingredient.id) }
    override fun getAllRecipesFlow(): Flow<List<Recipe>> = flowOf(emptyList())
    override suspend fun getRecipeForProduct(productId: String, variantId: String?): Recipe? = null
    override suspend fun getRecipeById(recipeId: String): Recipe? = null
    override suspend fun saveRecipe(recipe: Recipe) {}
    override suspend fun deleteRecipe(recipeId: String) {}
    override suspend fun calculateHppForProduct(productId: String, variantId: String?, sellingPrice: Double): HppBreakdown? = null
    override suspend fun seedInitialIngredientsAndRecipesIfEmpty() {}
}
