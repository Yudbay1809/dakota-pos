package com.dakota.pos

import com.dakota.pos.domain.inventory.IdempotentStockDeductionEngine
import com.dakota.pos.domain.model.inventory.MovementType
import com.dakota.pos.domain.model.inventory.StockMovement
import com.dakota.pos.domain.model.inventory.StockOpname
import com.dakota.pos.domain.model.recipe.Ingredient
import com.dakota.pos.domain.model.recipe.Recipe
import com.dakota.pos.domain.model.recipe.RecipeItem
import com.dakota.pos.domain.model.unit.UnitType
import com.dakota.pos.domain.repository.InventoryRepository
import com.dakota.pos.domain.repository.RecipeRepository
import com.dakota.pos.domain.usecase.inventory.ManualStockMovementUseCase
import com.dakota.pos.domain.usecase.inventory.PerformStockOpnameUseCase
import com.dakota.pos.domain.usecase.inventory.RecordWastageUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit tests for Phase 08: Inventory Engine.
 *
 * Test coverage:
 * 1. Normal deduction saat transaksi sukses
 * 2. Insufficient stock handling (stok minus / warning)
 * 3. Duplicate transaction idempotency (mencegah double cut)
 * 4. Cancellation / refund handling (pengembalian stok)
 * 5. Manual stock adjustment & opname
 * 6. Wastage recording
 * 7. Unit conversion in deduction
 */
class InventoryEngineTest {

    // ─── In-Memory Fakes ────────────────────────────────────────────────

    private lateinit var fakeInventoryRepo: FakeInventoryRepository
    private lateinit var fakeRecipeRepo: FakeRecipeRepository
    private lateinit var engine: IdempotentStockDeductionEngine

    @Before
    fun setup() {
        fakeInventoryRepo = FakeInventoryRepository()
        fakeRecipeRepo = FakeRecipeRepository()
        engine = IdempotentStockDeductionEngine(fakeInventoryRepo, fakeRecipeRepo)

        // Setup default test ingredients
        fakeInventoryRepo.ingredientsMap["ing-kopi"] = Ingredient(
            id = "ing-kopi", name = "Kopi Arabika", sku = "BB-001",
            defaultUnit = UnitType.G, costPerDefaultUnit = 250.0,
            currentStock = 5000.0, minimumStock = 500.0
        )
        fakeInventoryRepo.ingredientsMap["ing-susu"] = Ingredient(
            id = "ing-susu", name = "Susu Full Cream", sku = "BB-002",
            defaultUnit = UnitType.ML, costPerDefaultUnit = 20.0,
            currentStock = 10000.0, minimumStock = 2000.0
        )
        fakeInventoryRepo.ingredientsMap["ing-gula"] = Ingredient(
            id = "ing-gula", name = "Gula Pasir", sku = "BB-003",
            defaultUnit = UnitType.G, costPerDefaultUnit = 15.0,
            currentStock = 3000.0, minimumStock = 500.0
        )

        // Setup default recipe: Cafe Latte uses 18g kopi + 200ml susu + 10g gula
        fakeRecipeRepo.recipesMap["prod-latte"] = Recipe(
            id = "recipe-latte", productId = "prod-latte", productName = "Cafe Latte",
            items = listOf(
                RecipeItem("ri-1", "recipe-latte", "ing-kopi", "Kopi Arabika", 18.0, UnitType.G),
                RecipeItem("ri-2", "recipe-latte", "ing-susu", "Susu Full Cream", 200.0, UnitType.ML),
                RecipeItem("ri-3", "recipe-latte", "ing-gula", "Gula Pasir", 10.0, UnitType.G)
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    //  TEST 1: Normal Deduction
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `deduct stock for single item order - normal case`() = runBlocking {
        val result = engine.deductStockForOrder(
            orderId = "order-001",
            lineItems = listOf(
                IdempotentStockDeductionEngine.OrderLineItem("prod-latte", null, 1)
            ),
            userId = "user-1",
            userName = "Kasir A"
        )

        assertFalse("Should not be already processed", result.wasAlreadyProcessed)
        assertEquals("Should create 3 movements (one per ingredient)", 3, result.movements.size)
        assertTrue("No warnings expected", result.insufficientStockWarnings.isEmpty())

        // Verify stock levels
        val kopi = fakeInventoryRepo.ingredientsMap["ing-kopi"]!!
        assertEquals("Kopi should be 5000 - 18 = 4982", 4982.0, kopi.currentStock, 0.01)

        val susu = fakeInventoryRepo.ingredientsMap["ing-susu"]!!
        assertEquals("Susu should be 10000 - 200 = 9800", 9800.0, susu.currentStock, 0.01)

        val gula = fakeInventoryRepo.ingredientsMap["ing-gula"]!!
        assertEquals("Gula should be 3000 - 10 = 2990", 2990.0, gula.currentStock, 0.01)
    }

    @Test
    fun `deduct stock for multi-quantity order`() = runBlocking {
        val result = engine.deductStockForOrder(
            orderId = "order-002",
            lineItems = listOf(
                IdempotentStockDeductionEngine.OrderLineItem("prod-latte", null, 3) // 3 Lattes
            ),
            userId = "user-1",
            userName = "Kasir A"
        )

        assertFalse(result.wasAlreadyProcessed)

        val kopi = fakeInventoryRepo.ingredientsMap["ing-kopi"]!!
        assertEquals("Kopi: 5000 - (18*3) = 4946", 4946.0, kopi.currentStock, 0.01)

        val susu = fakeInventoryRepo.ingredientsMap["ing-susu"]!!
        assertEquals("Susu: 10000 - (200*3) = 9400", 9400.0, susu.currentStock, 0.01)
    }

    @Test
    fun `deduct stock aggregates same ingredient from multiple products`() = runBlocking {
        // Add second recipe that also uses kopi
        fakeRecipeRepo.recipesMap["prod-espresso"] = Recipe(
            id = "recipe-espresso", productId = "prod-espresso", productName = "Espresso",
            items = listOf(
                RecipeItem("ri-4", "recipe-espresso", "ing-kopi", "Kopi Arabika", 20.0, UnitType.G)
            )
        )

        val result = engine.deductStockForOrder(
            orderId = "order-003",
            lineItems = listOf(
                IdempotentStockDeductionEngine.OrderLineItem("prod-latte", null, 1),
                IdempotentStockDeductionEngine.OrderLineItem("prod-espresso", null, 2)
            ),
            userId = "user-1",
            userName = "Kasir A"
        )

        assertFalse(result.wasAlreadyProcessed)
        // Kopi: 18 (latte) + 20*2 (espresso) = 58g deducted
        val kopi = fakeInventoryRepo.ingredientsMap["ing-kopi"]!!
        assertEquals("Kopi: 5000 - 58 = 4942", 4942.0, kopi.currentStock, 0.01)
    }

    @Test
    fun `movements are recorded with correct type and reference`() = runBlocking {
        engine.deductStockForOrder(
            orderId = "order-004",
            lineItems = listOf(
                IdempotentStockDeductionEngine.OrderLineItem("prod-latte", null, 1)
            ),
            userId = "user-1",
            userName = "Kasir A"
        )

        val movements = fakeInventoryRepo.getMovementsByReferenceId("order-004")
        assertTrue("Should have recorded movements", movements.isNotEmpty())
        movements.forEach { m ->
            assertEquals("All movements should be SALE type", MovementType.SALE, m.type)
            assertEquals("Reference should be orderId", "order-004", m.referenceId)
            assertTrue("quantityChange should be negative", m.quantityChange < 0)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  TEST 2: Insufficient Stock Handling
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `insufficient stock produces warning but still deducts`() = runBlocking {
        // Set kopi stock very low
        fakeInventoryRepo.ingredientsMap["ing-kopi"] = fakeInventoryRepo.ingredientsMap["ing-kopi"]!!
            .copy(currentStock = 10.0) // Only 10g, needs 18g

        val result = engine.deductStockForOrder(
            orderId = "order-005",
            lineItems = listOf(
                IdempotentStockDeductionEngine.OrderLineItem("prod-latte", null, 1)
            ),
            userId = "user-1",
            userName = "Kasir A"
        )

        assertFalse(result.wasAlreadyProcessed)
        assertTrue("Should have insufficient stock warning", result.insufficientStockWarnings.isNotEmpty())

        val kopiWarning = result.insufficientStockWarnings.find { it.ingredientId == "ing-kopi" }
        assertNotNull("Kopi should have warning", kopiWarning)
        assertEquals(18.0, kopiWarning!!.requiredAmount, 0.01)
        assertEquals(10.0, kopiWarning.availableStock, 0.01)

        // Stock should go negative (business decision: allow, just warn)
        val kopi = fakeInventoryRepo.ingredientsMap["ing-kopi"]!!
        assertEquals("Kopi should be -8", -8.0, kopi.currentStock, 0.01)
    }

    @Test
    fun `zero stock produces warning`() = runBlocking {
        fakeInventoryRepo.ingredientsMap["ing-kopi"] = fakeInventoryRepo.ingredientsMap["ing-kopi"]!!
            .copy(currentStock = 0.0)

        val result = engine.deductStockForOrder(
            orderId = "order-006",
            lineItems = listOf(
                IdempotentStockDeductionEngine.OrderLineItem("prod-latte", null, 1)
            ),
            userId = "user-1",
            userName = "Kasir A"
        )

        assertTrue("Should have warnings", result.insufficientStockWarnings.isNotEmpty())
    }

    // ═══════════════════════════════════════════════════════════════════
    //  TEST 3: Idempotency (Double Trigger Prevention)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `idempotency - second call with same orderId does nothing`() = runBlocking {
        // First deduction
        val result1 = engine.deductStockForOrder(
            orderId = "order-007",
            lineItems = listOf(
                IdempotentStockDeductionEngine.OrderLineItem("prod-latte", null, 1)
            ),
            userId = "user-1",
            userName = "Kasir A"
        )
        assertFalse(result1.wasAlreadyProcessed)
        assertEquals(3, result1.movements.size)

        val kopiAfterFirst = fakeInventoryRepo.ingredientsMap["ing-kopi"]!!.currentStock

        // Second deduction with SAME orderId (simulating retry/duplicate trigger)
        val result2 = engine.deductStockForOrder(
            orderId = "order-007",
            lineItems = listOf(
                IdempotentStockDeductionEngine.OrderLineItem("prod-latte", null, 1)
            ),
            userId = "user-1",
            userName = "Kasir A"
        )

        assertTrue("Second call should be flagged as already processed", result2.wasAlreadyProcessed)
        assertTrue("Second call should return no movements", result2.movements.isEmpty())

        // Stock should NOT have changed
        val kopiAfterSecond = fakeInventoryRepo.ingredientsMap["ing-kopi"]!!.currentStock
        assertEquals("Stock should not change on duplicate", kopiAfterFirst, kopiAfterSecond, 0.001)
    }

    @Test
    fun `idempotency - different orderIds are processed independently`() = runBlocking {
        engine.deductStockForOrder(
            orderId = "order-A",
            lineItems = listOf(
                IdempotentStockDeductionEngine.OrderLineItem("prod-latte", null, 1)
            ),
            userId = "user-1", userName = "Kasir A"
        )

        val result2 = engine.deductStockForOrder(
            orderId = "order-B",
            lineItems = listOf(
                IdempotentStockDeductionEngine.OrderLineItem("prod-latte", null, 1)
            ),
            userId = "user-1", userName = "Kasir A"
        )

        assertFalse("Different orderId should not be blocked", result2.wasAlreadyProcessed)
        assertEquals(3, result2.movements.size)

        // Kopi should be deducted twice: 5000 - 18 - 18 = 4964
        val kopi = fakeInventoryRepo.ingredientsMap["ing-kopi"]!!
        assertEquals(4964.0, kopi.currentStock, 0.01)
    }

    // ═══════════════════════════════════════════════════════════════════
    //  TEST 4: Cancellation / Refund (Return Stock)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `return stock on cancel reverses deduction`() = runBlocking {
        val initialKopi = fakeInventoryRepo.ingredientsMap["ing-kopi"]!!.currentStock

        // Deduct
        engine.deductStockForOrder(
            orderId = "order-cancel-001",
            lineItems = listOf(
                IdempotentStockDeductionEngine.OrderLineItem("prod-latte", null, 2)
            ),
            userId = "user-1", userName = "Kasir A"
        )

        val afterDeduction = fakeInventoryRepo.ingredientsMap["ing-kopi"]!!.currentStock
        assertEquals(initialKopi - 36.0, afterDeduction, 0.01) // 18 * 2

        // Return
        val returnResult = engine.returnStockForOrder(
            orderId = "order-cancel-001",
            userId = "user-1", userName = "Kasir A"
        )

        assertFalse(returnResult.wasAlreadyProcessed)
        assertTrue("Should create RETURN movements", returnResult.movements.isNotEmpty())
        returnResult.movements.forEach { m ->
            assertEquals(MovementType.RETURN, m.type)
            assertTrue("Return quantityChange should be positive", m.quantityChange > 0)
        }

        // Stock should be restored
        val afterReturn = fakeInventoryRepo.ingredientsMap["ing-kopi"]!!.currentStock
        assertEquals("Stock should be restored to original", initialKopi, afterReturn, 0.01)
    }

    @Test
    fun `return is idempotent - second return does nothing`() = runBlocking {
        engine.deductStockForOrder(
            orderId = "order-cancel-002",
            lineItems = listOf(
                IdempotentStockDeductionEngine.OrderLineItem("prod-latte", null, 1)
            ),
            userId = "user-1", userName = "Kasir A"
        )

        // First return
        engine.returnStockForOrder("order-cancel-002", "user-1", "Kasir A")

        val stockAfterFirstReturn = fakeInventoryRepo.ingredientsMap["ing-kopi"]!!.currentStock

        // Second return (should be idempotent)
        val result2 = engine.returnStockForOrder("order-cancel-002", "user-1", "Kasir A")
        assertTrue("Second return should be flagged as already processed", result2.wasAlreadyProcessed)

        val stockAfterSecondReturn = fakeInventoryRepo.ingredientsMap["ing-kopi"]!!.currentStock
        assertEquals("Stock should not change on duplicate return", stockAfterFirstReturn, stockAfterSecondReturn, 0.001)
    }

    @Test
    fun `return on non-existent order does nothing`() = runBlocking {
        val result = engine.returnStockForOrder("order-never-existed", "user-1", "Kasir A")
        assertTrue(result.wasAlreadyProcessed)
        assertTrue(result.movements.isEmpty())
    }

    // ═══════════════════════════════════════════════════════════════════
    //  TEST 5: Manual Stock Adjustment & Opname
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `manual stock in increases stock and records movement`() = runBlocking {
        val useCase = ManualStockMovementUseCase(fakeInventoryRepo)
        val initialStock = fakeInventoryRepo.ingredientsMap["ing-kopi"]!!.currentStock

        useCase.stockIn("ing-kopi", 500.0, "Pembelian biji kopi", "user-1", "Admin")

        val newStock = fakeInventoryRepo.ingredientsMap["ing-kopi"]!!.currentStock
        assertEquals(initialStock + 500.0, newStock, 0.01)

        // Check movement record
        val movements = fakeInventoryRepo.movementsList.filter { it.ingredientId == "ing-kopi" && it.type == MovementType.IN }
        assertEquals(1, movements.size)
        assertEquals(500.0, movements.first().quantityChange, 0.01)
    }

    @Test
    fun `manual stock out decreases stock and records movement`() = runBlocking {
        val useCase = ManualStockMovementUseCase(fakeInventoryRepo)
        val initialStock = fakeInventoryRepo.ingredientsMap["ing-susu"]!!.currentStock

        useCase.stockOut("ing-susu", 1000.0, "Dipakai staff", "user-1", "Admin")

        val newStock = fakeInventoryRepo.ingredientsMap["ing-susu"]!!.currentStock
        assertEquals(initialStock - 1000.0, newStock, 0.01)

        val movements = fakeInventoryRepo.movementsList.filter { it.ingredientId == "ing-susu" && it.type == MovementType.OUT }
        assertEquals(1, movements.size)
        assertEquals(-1000.0, movements.first().quantityChange, 0.01)
    }

    @Test
    fun `stock opname adjusts stock to physical count and records adjustments`() = runBlocking {
        val useCase = PerformStockOpnameUseCase(fakeInventoryRepo)

        // Kopi system: 5000g, physical: 4800g (selisih -200g)
        // Susu system: 10000ml, physical: 10500ml (selisih +500ml)
        // Gula system: 3000g, physical: 3000g (no diff, no adjustment)
        val opname = useCase(
            physicalCounts = mapOf(
                "ing-kopi" to 4800.0,
                "ing-susu" to 10500.0,
                "ing-gula" to 3000.0
            ),
            userId = "user-1",
            userName = "Admin"
        )

        assertEquals(3, opname.items.size)

        // Verify stock adjusted to physical
        assertEquals(4800.0, fakeInventoryRepo.ingredientsMap["ing-kopi"]!!.currentStock, 0.01)
        assertEquals(10500.0, fakeInventoryRepo.ingredientsMap["ing-susu"]!!.currentStock, 0.01)
        assertEquals(3000.0, fakeInventoryRepo.ingredientsMap["ing-gula"]!!.currentStock, 0.01)

        // Verify adjustment movements only for non-zero differences
        val adjustments = fakeInventoryRepo.movementsList.filter { it.type == MovementType.ADJUSTMENT }
        assertEquals("Should have 2 adjustments (kopi and susu, not gula)", 2, adjustments.size)

        val kopiAdj = adjustments.find { it.ingredientId == "ing-kopi" }!!
        assertEquals(-200.0, kopiAdj.quantityChange, 0.01)

        val susuAdj = adjustments.find { it.ingredientId == "ing-susu" }!!
        assertEquals(500.0, susuAdj.quantityChange, 0.01)
    }

    @Test
    fun `opname saves to repository`() = runBlocking {
        val useCase = PerformStockOpnameUseCase(fakeInventoryRepo)
        val opname = useCase(
            physicalCounts = mapOf("ing-kopi" to 4900.0),
            userId = "user-1",
            userName = "Admin"
        )

        assertEquals(1, fakeInventoryRepo.opnamesList.size)
        assertEquals(opname.id, fakeInventoryRepo.opnamesList.first().id)
    }

    // ═══════════════════════════════════════════════════════════════════
    //  TEST 6: Wastage Recording
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `wastage recording deducts stock and records WASTAGE movement`() = runBlocking {
        val useCase = RecordWastageUseCase(fakeInventoryRepo)
        val initialStock = fakeInventoryRepo.ingredientsMap["ing-susu"]!!.currentStock

        useCase("ing-susu", 500.0, "Tumpah di counter", "user-1", "Barista")

        val newStock = fakeInventoryRepo.ingredientsMap["ing-susu"]!!.currentStock
        assertEquals(initialStock - 500.0, newStock, 0.01)

        val wastageMovements = fakeInventoryRepo.movementsList.filter { it.type == MovementType.WASTAGE }
        assertEquals(1, wastageMovements.size)
        assertEquals(-500.0, wastageMovements.first().quantityChange, 0.01)
        assertTrue(wastageMovements.first().notes!!.contains("Tumpah"))
    }

    // ═══════════════════════════════════════════════════════════════════
    //  TEST 7: Unit Conversion in Deduction
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `deduction converts recipe unit to ingredient default unit`() = runBlocking {
        // Recipe uses KG but ingredient default is G
        fakeRecipeRepo.recipesMap["prod-heavy"] = Recipe(
            id = "recipe-heavy", productId = "prod-heavy", productName = "Heavy Coffee",
            items = listOf(
                RecipeItem("ri-5", "recipe-heavy", "ing-kopi", "Kopi Arabika", 0.5, UnitType.KG) // 0.5 kg = 500g
            )
        )

        val result = engine.deductStockForOrder(
            orderId = "order-convert-001",
            lineItems = listOf(
                IdempotentStockDeductionEngine.OrderLineItem("prod-heavy", null, 1)
            ),
            userId = "user-1", userName = "Kasir A"
        )

        assertFalse(result.wasAlreadyProcessed)
        val kopi = fakeInventoryRepo.ingredientsMap["ing-kopi"]!!
        assertEquals("0.5kg = 500g should be deducted", 4500.0, kopi.currentStock, 0.01)
    }

    @Test
    fun `deduction with volume unit conversion (L to ML)`() = runBlocking {
        // Recipe uses L but ingredient default is ML
        fakeRecipeRepo.recipesMap["prod-bulk-milk"] = Recipe(
            id = "recipe-bulk", productId = "prod-bulk-milk", productName = "Bulk Milk Order",
            items = listOf(
                RecipeItem("ri-6", "recipe-bulk", "ing-susu", "Susu Full Cream", 1.5, UnitType.L) // 1.5L = 1500ml
            )
        )

        engine.deductStockForOrder(
            orderId = "order-convert-002",
            lineItems = listOf(
                IdempotentStockDeductionEngine.OrderLineItem("prod-bulk-milk", null, 1)
            ),
            userId = "user-1", userName = "Kasir A"
        )

        val susu = fakeInventoryRepo.ingredientsMap["ing-susu"]!!
        assertEquals("1.5L = 1500ml deducted", 8500.0, susu.currentStock, 0.01)
    }

    // ═══════════════════════════════════════════════════════════════════
    //  TEST 8: Edge Cases
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `product without recipe is skipped gracefully`() = runBlocking {
        val result = engine.deductStockForOrder(
            orderId = "order-no-recipe",
            lineItems = listOf(
                IdempotentStockDeductionEngine.OrderLineItem("prod-nonexistent", null, 1)
            ),
            userId = "user-1", userName = "Kasir A"
        )

        assertFalse(result.wasAlreadyProcessed)
        assertTrue("No movements if product has no recipe", result.movements.isEmpty())
    }

    @Test
    fun `empty order produces no movements`() = runBlocking {
        val result = engine.deductStockForOrder(
            orderId = "order-empty",
            lineItems = emptyList(),
            userId = "user-1", userName = "Kasir A"
        )

        assertFalse(result.wasAlreadyProcessed)
        assertTrue(result.movements.isEmpty())
    }

    @Test
    fun `manual stock in fails for non-existent ingredient`() = runBlocking {
        val useCase = ManualStockMovementUseCase(fakeInventoryRepo)
        try {
            useCase.stockIn("ing-fake", 100.0, null, "user-1", "Admin")
            fail("Should throw exception for non-existent ingredient")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("tidak ditemukan"))
        }
    }

    @Test
    fun `balance before and after are correctly tracked in movements`() = runBlocking {
        engine.deductStockForOrder(
            orderId = "order-balance-check",
            lineItems = listOf(
                IdempotentStockDeductionEngine.OrderLineItem("prod-latte", null, 1)
            ),
            userId = "user-1", userName = "Kasir A"
        )

        val kopiMovement = fakeInventoryRepo.movementsList.find { it.ingredientId == "ing-kopi" }!!
        assertEquals(5000.0, kopiMovement.balanceBefore, 0.01)
        assertEquals(4982.0, kopiMovement.balanceAfter, 0.01)
    }

    // ─── FAKE IMPLEMENTATIONS ──────────────────────────────────────────

    private class FakeInventoryRepository : InventoryRepository {
        val ingredientsMap = mutableMapOf<String, Ingredient>()
        val movementsList = mutableListOf<StockMovement>()
        val opnamesList = mutableListOf<StockOpname>()
        private val saleOrderIds = mutableSetOf<String>()

        override suspend fun recordMovement(movement: StockMovement) {
            movementsList.add(movement)
            if (movement.type == MovementType.SALE) {
                movement.referenceId?.let { saleOrderIds.add(it) }
            }
        }

        override suspend fun recordMovements(movements: List<StockMovement>) {
            movements.forEach { recordMovement(it) }
        }

        override fun getMovementsForIngredient(ingredientId: String): Flow<List<StockMovement>> {
            return flowOf(movementsList.filter { it.ingredientId == ingredientId })
        }

        override fun getRecentMovements(limit: Int): Flow<List<StockMovement>> {
            return flowOf(movementsList.sortedByDescending { it.timestamp }.take(limit))
        }

        override fun getMovementsByType(type: MovementType): Flow<List<StockMovement>> {
            return flowOf(movementsList.filter { it.type == type })
        }

        override suspend fun getMovementsByReferenceId(referenceId: String): List<StockMovement> {
            return movementsList.filter { it.referenceId == referenceId }
        }

        override suspend fun hasBeenDeductedForOrder(orderId: String): Boolean {
            return movementsList.any { it.referenceId == orderId && it.type == MovementType.SALE }
        }

        override suspend fun saveStockOpname(opname: StockOpname) {
            opnamesList.add(opname)
        }

        override fun getAllOpnamesFlow(): Flow<List<StockOpname>> = flowOf(opnamesList)

        override suspend fun updateIngredientStock(ingredientId: String, newStock: Double) {
            ingredientsMap[ingredientId]?.let {
                ingredientsMap[ingredientId] = it.copy(currentStock = newStock)
            }
        }

        override suspend fun getIngredientById(ingredientId: String): Ingredient? {
            return ingredientsMap[ingredientId]
        }

        override suspend fun getAllIngredients(): List<Ingredient> = ingredientsMap.values.toList()

        override fun getAllIngredientsFlow(): Flow<List<Ingredient>> = flowOf(ingredientsMap.values.toList())
    }

    private class FakeRecipeRepository : RecipeRepository {
        val recipesMap = mutableMapOf<String, Recipe>() // key = productId

        override fun getAllIngredientsFlow() = flowOf(emptyList<Ingredient>())
        override suspend fun getAllIngredients() = emptyList<Ingredient>()
        override suspend fun getIngredientById(id: String): Ingredient? = null
        override suspend fun saveIngredient(ingredient: Ingredient) {}
        override suspend fun deleteIngredient(ingredient: Ingredient) {}
        override fun getAllRecipesFlow() = flowOf(recipesMap.values.toList())
        override suspend fun getRecipeForProduct(productId: String, variantId: String?): Recipe? = recipesMap[productId]
        override suspend fun getRecipeById(recipeId: String) = recipesMap.values.find { it.id == recipeId }
        override suspend fun saveRecipe(recipe: Recipe) { recipesMap[recipe.productId] = recipe }
        override suspend fun deleteRecipe(recipeId: String) {}
        override suspend fun calculateHppForProduct(productId: String, variantId: String?, sellingPrice: Double) = null
        override suspend fun seedInitialIngredientsAndRecipesIfEmpty() {}
    }
}
