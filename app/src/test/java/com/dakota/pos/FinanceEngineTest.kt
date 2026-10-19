package com.dakota.pos

import com.dakota.pos.domain.model.Cart
import com.dakota.pos.domain.model.Order
import com.dakota.pos.domain.model.OrderItem
import com.dakota.pos.domain.model.OrderStatus
import com.dakota.pos.domain.model.finance.ExpenseCategory
import com.dakota.pos.domain.model.finance.OperatingExpense
import com.dakota.pos.domain.model.recipe.HppBreakdown
import com.dakota.pos.domain.model.recipe.Ingredient
import com.dakota.pos.domain.model.recipe.Recipe
import com.dakota.pos.domain.repository.FinanceRepository
import com.dakota.pos.domain.repository.OrderRepository
import com.dakota.pos.domain.repository.RecipeRepository
import com.dakota.pos.domain.usecase.finance.DeleteOperatingExpenseUseCase
import com.dakota.pos.domain.usecase.finance.GenerateProfitLossReportUseCase
import com.dakota.pos.domain.usecase.finance.GetOperatingExpensesUseCase
import com.dakota.pos.domain.usecase.finance.RecordOperatingExpenseUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Unit Tests for Phase 12: Finance + Profit Engine.
 *
 * Test coverage:
 * 1. Net Sales formula: Gross Sales - Discount - Refund
 * 2. COGS calculation from recipe HPP formula
 * 3. Gross Profit & Gross Margin % calculation
 * 4. Operating Expenses breakdown by category
 * 5. Net Profit calculation: Gross Profit - Total Operating Expenses
 * 6. Loss scenario (Operating Expenses > Gross Profit)
 * 7. Expense input validation (Rejection of amount <= 0 or blank description)
 * 8. Expense deletion functionality
 * 9. Menu item profitability ranking & stats
 * 10. Empty period edge case (Zero sales, no divide-by-zero crash)
 */
class FinanceEngineTest {

    private lateinit var fakeOrderRepo: FakeOrderRepository
    private lateinit var fakeRecipeRepo: FakeRecipeRepository
    private lateinit var fakeFinanceRepo: FakeFinanceRepository

    private lateinit var generateProfitLossReportUseCase: GenerateProfitLossReportUseCase
    private lateinit var recordOperatingExpenseUseCase: RecordOperatingExpenseUseCase
    private lateinit var deleteOperatingExpenseUseCase: DeleteOperatingExpenseUseCase
    private lateinit var getOperatingExpensesUseCase: GetOperatingExpensesUseCase

    @Before
    fun setup() {
        fakeOrderRepo = FakeOrderRepository()
        fakeRecipeRepo = FakeRecipeRepository()
        fakeFinanceRepo = FakeFinanceRepository()

        generateProfitLossReportUseCase = GenerateProfitLossReportUseCase(
            orderRepository = fakeOrderRepo,
            recipeRepository = fakeRecipeRepo,
            financeRepository = fakeFinanceRepo
        )
        recordOperatingExpenseUseCase = RecordOperatingExpenseUseCase(fakeFinanceRepo)
        deleteOperatingExpenseUseCase = DeleteOperatingExpenseUseCase(fakeFinanceRepo)
        getOperatingExpensesUseCase = GetOperatingExpensesUseCase(fakeFinanceRepo)

        // Setup mock HPP breakdowns
        // Latte: selling price 30,000, recipe HPP 9,000
        fakeRecipeRepo.hppBreakdownMap["prod_latte"] = HppBreakdown(
            productId = "prod_latte",
            productName = "Caffe Latte",
            sellingPrice = 30000.0,
            totalHpp = 9000.0,
            grossProfit = 21000.0,
            grossMarginPercentage = 70.0,
            itemBreakdowns = emptyList()
        )
        // Croissant: selling price 25,000, recipe HPP 10,000
        fakeRecipeRepo.hppBreakdownMap["prod_croissant"] = HppBreakdown(
            productId = "prod_croissant",
            productName = "Butter Croissant",
            sellingPrice = 25000.0,
            totalHpp = 10000.0,
            grossProfit = 15000.0,
            grossMarginPercentage = 60.0,
            itemBreakdowns = emptyList()
        )
    }

    // ========================================================================
    // 1. FORMULA & PROFIT TESTS
    // ========================================================================

    @Test
    fun testNetSalesAndCogsAndGrossProfit() = runBlocking {
        val now = System.currentTimeMillis()

        // Order 1: 2x Latte (Subtotal 60,000, Diskon 5,000, Total 55,000)
        // Order 2: 1x Croissant (Subtotal 25,000, Diskon 0, Total 25,000)
        fakeOrderRepo.paidOrders = listOf(
            Order(
                id = "ord-1",
                orderNumber = "DKT-001",
                cashierId = "cashier-1",
                cashierName = "Budi",
                status = OrderStatus.PAID,
                subtotal = 60000.0,
                discount = 5000.0,
                total = 55000.0,
                createdAt = now - 1000,
                items = listOf(
                    OrderItem(id = "i1", orderId = "ord-1", productId = "prod_latte", productName = "Caffe Latte", quantity = 2, unitPrice = 30000.0, subtotal = 60000.0)
                )
            ),
            Order(
                id = "ord-2",
                orderNumber = "DKT-002",
                cashierId = "cashier-1",
                cashierName = "Budi",
                status = OrderStatus.PAID,
                subtotal = 25000.0,
                discount = 0.0,
                total = 25000.0,
                createdAt = now - 500,
                items = listOf(
                    OrderItem(id = "i2", orderId = "ord-2", productId = "prod_croissant", productName = "Butter Croissant", quantity = 1, unitPrice = 25000.0, subtotal = 25000.0)
                )
            )
        )

        val report = generateProfitLossReportUseCase(now - 10000, now + 10000)

        // Gross Sales = 60,000 + 25,000 = 85,000
        assertEquals(85000.0, report.grossSales, 0.001)
        // Total Discount = 5,000
        assertEquals(5000.0, report.totalDiscount, 0.001)
        // Net Sales = 85,000 - 5,000 = 80,000
        assertEquals(80000.0, report.netSales, 0.001)

        // COGS = (2 x 9,000) + (1 x 10,000) = 18,000 + 10,000 = 28,000
        assertEquals(28000.0, report.totalCogs, 0.001)

        // Gross Profit = Net Sales (80,000) - COGS (28,000) = 52,000
        assertEquals(52000.0, report.grossProfit, 0.001)

        // Gross Margin % = (52,000 / 80,000) * 100% = 65.0%
        assertEquals(65.0, report.grossProfitMargin, 0.001)

        // Total orders & AOV
        assertEquals(2, report.totalOrdersCount)
        assertEquals(3, report.totalItemsSold)
        assertEquals(40000.0, report.averageOrderValue, 0.001) // 80,000 / 2
    }

    @Test
    fun testNetProfitWithOperatingExpenses() = runBlocking {
        val now = System.currentTimeMillis()

        // Sales: Net Sales = 100,000, COGS = 30,000 -> Gross Profit = 70,000
        fakeOrderRepo.paidOrders = listOf(
            Order(
                id = "ord-1",
                orderNumber = "DKT-001",
                cashierId = "c1",
                cashierName = "Budi",
                status = OrderStatus.PAID,
                subtotal = 100000.0,
                discount = 0.0,
                total = 100000.0,
                createdAt = now,
                items = listOf(
                    OrderItem(id = "i1", orderId = "ord-1", productId = "prod_latte", productName = "Caffe Latte", quantity = 3, unitPrice = 30000.0, subtotal = 90000.0)
                )
            )
        )
        // Mock COGS: 3 * 9,000 = 27,000
        // Net Sales = 100,000 -> Gross Profit = 73,000

        // Catat Biaya Operasional:
        // Listrik: 20,000
        // Internet: 15,000
        // Cleaning: 5,000
        // Total Beban = 40,000
        recordOperatingExpenseUseCase(ExpenseCategory.ELECTRICITY, 20000.0, "Token Listrik", "Owner", date = now)
        recordOperatingExpenseUseCase(ExpenseCategory.INTERNET, 15000.0, "Paket Internet Toko", "Owner", date = now)
        recordOperatingExpenseUseCase(ExpenseCategory.CLEANING, 5000.0, "Sabun Cuci", "Kasir", date = now)

        val report = generateProfitLossReportUseCase(now - 10000, now + 10000)

        assertEquals(100000.0, report.netSales, 0.001)
        assertEquals(27000.0, report.totalCogs, 0.001)
        assertEquals(73000.0, report.grossProfit, 0.001)

        // Operating Expenses
        assertEquals(40000.0, report.totalOperatingExpenses, 0.001)
        assertEquals(3, report.operatingExpenses.size)
        assertEquals(20000.0, report.expenseBreakdown[ExpenseCategory.ELECTRICITY] ?: 0.0, 0.001)
        assertEquals(15000.0, report.expenseBreakdown[ExpenseCategory.INTERNET] ?: 0.0, 0.001)
        assertEquals(5000.0, report.expenseBreakdown[ExpenseCategory.CLEANING] ?: 0.0, 0.001)

        // Net Profit = Gross Profit (73,000) - Operating Expenses (40,000) = 33,000
        assertEquals(33000.0, report.netProfit, 0.001)
        assertEquals(33.0, report.netProfitMargin, 0.001) // 33,000 / 100,000 * 100%
        assertTrue(report.isProfitable)
    }

    @Test
    fun testNetLossScenarioWhenExpensesExceedGrossProfit() = runBlocking {
        val now = System.currentTimeMillis()

        // Gross Profit = 20,000
        fakeOrderRepo.paidOrders = listOf(
            Order(
                id = "ord-1", orderNumber = "DKT-001", cashierId = "c1", cashierName = "Budi",
                status = OrderStatus.PAID, subtotal = 50000.0, total = 50000.0, createdAt = now,
                items = listOf(
                    OrderItem(id = "i1", orderId = "ord-1", productId = "prod_latte", productName = "Caffe Latte", quantity = 1, unitPrice = 30000.0, subtotal = 30000.0)
                )
            )
        )
        // Net Sales = 50,000, COGS = 9,000 -> Gross Profit = 41,000

        // Biaya Operasional sewa tempat = 100,000 (Lebih besar dari laba kotor)
        recordOperatingExpenseUseCase(ExpenseCategory.RENT, 100000.0, "Sewa Tempat Harian", "Owner", date = now)

        val report = generateProfitLossReportUseCase(now - 10000, now + 10000)

        assertEquals(41000.0, report.grossProfit, 0.001)
        assertEquals(100000.0, report.totalOperatingExpenses, 0.001)

        // Net Profit = 41,000 - 100,000 = -59,000 (RUGI)
        assertEquals(-59000.0, report.netProfit, 0.001)
        assertEquals(-118.0, report.netProfitMargin, 0.001)
        assertFalse(report.isProfitable)
    }

    // ========================================================================
    // 2. EXPENSE VALIDATION & MANAGEMENT TESTS
    // ========================================================================

    @Test
    fun testRecordExpenseValidationRejectsInvalidAmount() = runBlocking {
        val resultZero = recordOperatingExpenseUseCase(ExpenseCategory.WATER, 0.0, "Air Galon", "Staff")
        assertTrue(resultZero.isFailure)
        assertTrue(resultZero.exceptionOrNull()?.message?.contains("harus lebih besar dari 0") == true)

        val resultNegative = recordOperatingExpenseUseCase(ExpenseCategory.WATER, -15000.0, "Air Galon", "Staff")
        assertTrue(resultNegative.isFailure)
    }

    @Test
    fun testRecordExpenseValidationRejectsBlankDescription() = runBlocking {
        val result = recordOperatingExpenseUseCase(ExpenseCategory.OTHER, 25000.0, "   ", "Staff")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("tidak boleh kosong") == true)
    }

    @Test
    fun testDeleteExpenseRemovesExpenseFromCalculation() = runBlocking {
        val now = System.currentTimeMillis()
        val exp = recordOperatingExpenseUseCase(ExpenseCategory.MARKETING, 50000.0, "Iklan IG Ads", "Manager", date = now).getOrThrow()

        var report = generateProfitLossReportUseCase(now - 1000, now + 1000)
        assertEquals(50000.0, report.totalOperatingExpenses, 0.001)

        // Hapus biaya
        val delResult = deleteOperatingExpenseUseCase(exp.id)
        assertTrue(delResult.isSuccess)

        report = generateProfitLossReportUseCase(now - 1000, now + 1000)
        assertEquals(0.0, report.totalOperatingExpenses, 0.001)
        assertTrue(report.operatingExpenses.isEmpty())
    }

    // ========================================================================
    // 3. PRODUCT PROFITABILITY & EDGE CASES
    // ========================================================================

    @Test
    fun testProductProfitabilityRanking() = runBlocking {
        val now = System.currentTimeMillis()

        // Latte: 3x @ 30k = 90k omzet, COGS = 27k, Profit = 63k
        // Croissant: 1x @ 25k = 25k omzet, COGS = 10k, Profit = 15k
        fakeOrderRepo.paidOrders = listOf(
            Order(
                id = "o1", orderNumber = "1", cashierId = "c", cashierName = "C",
                status = OrderStatus.PAID, subtotal = 115000.0, total = 115000.0, createdAt = now,
                items = listOf(
                    OrderItem(id = "1", orderId = "o1", productId = "prod_latte", productName = "Caffe Latte", quantity = 3, unitPrice = 30000.0, subtotal = 90000.0),
                    OrderItem(id = "2", orderId = "o1", productId = "prod_croissant", productName = "Butter Croissant", quantity = 1, unitPrice = 25000.0, subtotal = 25000.0)
                )
            )
        )

        val report = generateProfitLossReportUseCase(now - 1000, now + 1000)

        assertEquals(2, report.topProducts.size)
        val top1 = report.topProducts[0]
        assertEquals("prod_latte", top1.productId)
        assertEquals(3, top1.quantitySold)
        assertEquals(90000.0, top1.revenue, 0.001)
        assertEquals(27000.0, top1.cogs, 0.001)
        assertEquals(63000.0, top1.grossProfit, 0.001)
        assertEquals(70.0, top1.marginPercent, 0.001)

        val top2 = report.topProducts[1]
        assertEquals("prod_croissant", top2.productId)
        assertEquals(1, top2.quantitySold)
        assertEquals(25000.0, top2.revenue, 0.001)
        assertEquals(10000.0, top2.cogs, 0.001)
        assertEquals(15000.0, top2.grossProfit, 0.001)
        assertEquals(60.0, top2.marginPercent, 0.001)
    }

    @Test
    fun testEmptyPeriodReturnsZeroSafelyWithoutDivideByZero() = runBlocking {
        val now = System.currentTimeMillis()
        fakeOrderRepo.paidOrders = emptyList()

        val report = generateProfitLossReportUseCase(now - 10000, now)

        assertEquals(0.0, report.grossSales, 0.001)
        assertEquals(0.0, report.netSales, 0.001)
        assertEquals(0.0, report.totalCogs, 0.001)
        assertEquals(0.0, report.grossProfit, 0.001)
        assertEquals(0.0, report.grossProfitMargin, 0.001)
        assertEquals(0.0, report.netProfit, 0.001)
        assertEquals(0.0, report.netProfitMargin, 0.001)
        assertEquals(0, report.totalOrdersCount)
        assertEquals(0.0, report.averageOrderValue, 0.001)
        assertTrue(report.topProducts.isEmpty())
    }

    // ========================================================================
    // TEST DOUBLES (FAKES)
    // ========================================================================

    private class FakeOrderRepository : OrderRepository {
        var paidOrders = listOf<Order>()

        override suspend fun createOrderFromCart(cart: Cart, cashierId: String, cashierName: String, initialStatus: OrderStatus): Order {
            throw NotImplementedError()
        }

        override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Boolean = true
        override suspend fun markOrderAsPaid(orderId: String, paymentMethod: String): Boolean = true
        override suspend fun getOrderById(orderId: String): Order? = paidOrders.find { it.id == orderId }
        override fun getRecentOrders(): Flow<List<Order>> = flowOf(paidOrders)
        override suspend fun getPaidOrdersBetween(startTime: Long, endTime: Long): List<Order> {
            return paidOrders.filter { it.createdAt in startTime..endTime }
        }
    }

    private class FakeRecipeRepository : RecipeRepository {
        val hppBreakdownMap = mutableMapOf<String, HppBreakdown>()

        override fun getAllIngredientsFlow(): Flow<List<Ingredient>> = flowOf(emptyList())
        override suspend fun getAllIngredients(): List<Ingredient> = emptyList()
        override suspend fun getIngredientById(id: String): Ingredient? = null
        override suspend fun saveIngredient(ingredient: Ingredient) {}
        override suspend fun deleteIngredient(ingredient: Ingredient) {}
        override fun getAllRecipesFlow(): Flow<List<Recipe>> = flowOf(emptyList())
        override suspend fun getRecipeForProduct(productId: String, variantId: String?): Recipe? = null
        override suspend fun getRecipeById(recipeId: String): Recipe? = null
        override suspend fun saveRecipe(recipe: Recipe) {}
        override suspend fun deleteRecipe(recipeId: String) {}

        override suspend fun calculateHppForProduct(
            productId: String,
            variantId: String?,
            sellingPrice: Double
        ): HppBreakdown? {
            return hppBreakdownMap[productId]
        }

        override suspend fun seedInitialIngredientsAndRecipesIfEmpty() {}
    }

    private class FakeFinanceRepository : FinanceRepository {
        val expenses = mutableMapOf<String, OperatingExpense>()

        override suspend fun saveExpense(expense: OperatingExpense) {
            expenses[expense.id] = expense
        }

        override suspend fun getExpenseById(id: String): OperatingExpense? = expenses[id]

        override suspend fun deleteExpense(id: String) {
            expenses.remove(id)
        }

        override suspend fun getExpensesBetween(startTime: Long, endTime: Long): List<OperatingExpense> {
            return expenses.values.filter { it.date in startTime..endTime }
        }

        override fun getExpensesBetweenFlow(startTime: Long, endTime: Long): Flow<List<OperatingExpense>> {
            return flowOf(expenses.values.filter { it.date in startTime..endTime })
        }

        override fun getAllExpensesFlow(): Flow<List<OperatingExpense>> = flowOf(expenses.values.toList())

        override suspend fun getTotalExpensesBetween(startTime: Long, endTime: Long): Double {
            return expenses.values.filter { it.date in startTime..endTime }.sumOf { it.amount }
        }
    }
}
