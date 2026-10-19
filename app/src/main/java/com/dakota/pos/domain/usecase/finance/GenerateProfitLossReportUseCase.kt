package com.dakota.pos.domain.usecase.finance

import com.dakota.pos.domain.model.finance.ExpenseCategory
import com.dakota.pos.domain.model.finance.OperatingExpense
import com.dakota.pos.domain.model.finance.ProductSalesMetric
import com.dakota.pos.domain.model.finance.ProfitLossReport
import com.dakota.pos.domain.repository.FinanceRepository
import com.dakota.pos.domain.repository.OrderRepository
import com.dakota.pos.domain.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GenerateProfitLossReportUseCase(
    private val orderRepository: OrderRepository,
    private val recipeRepository: RecipeRepository,
    private val financeRepository: FinanceRepository
) {
    suspend operator fun invoke(
        startTime: Long,
        endTime: Long
    ): ProfitLossReport = withContext(Dispatchers.IO) {
        // 1. Ambil pesanan yang berstatus PAID dalam rentang waktu
        val orders = orderRepository.getPaidOrdersBetween(startTime, endTime)

        // 2. Hitung Gross Sales, Diskon, Refund, dan Net Sales
        val grossSales = orders.sumOf { it.subtotal }
        val totalDiscount = orders.sumOf { it.discount }
        val totalRefund = 0.0 // Refund dapat diperluas jika ada cancellation order
        val netSales = (grossSales - totalDiscount - totalRefund).coerceAtLeast(0.0)

        // 3. Hitung COGS (HPP Terjual) berdasarkan resep formula bahan baku
        val hppCache = mutableMapOf<String, Double>()
        var totalCogs = 0.0
        val productStatMap = mutableMapOf<String, ProductStatAccumulator>()

        orders.forEach { order ->
            order.items.forEach { item ->
                val cacheKey = "${item.productId}_${item.variantId ?: ""}"
                val hppPerUnit = hppCache.getOrPut(cacheKey) {
                    val breakdown = recipeRepository.calculateHppForProduct(
                        productId = item.productId,
                        variantId = item.variantId,
                        sellingPrice = item.unitPrice
                    )
                    breakdown?.totalHpp ?: 0.0
                }

                val itemTotalCogs = hppPerUnit * item.quantity
                totalCogs += itemTotalCogs

                val stat = productStatMap.getOrPut(item.productId) {
                    ProductStatAccumulator(
                        productId = item.productId,
                        productName = item.productName
                    )
                }
                stat.quantity += item.quantity
                stat.revenue += item.subtotal
                stat.cogs += itemTotalCogs
            }
        }

        // 4. Hitung Laba Kotor (Gross Profit) dan Margin Laba Kotor
        val grossProfit = netSales - totalCogs
        val grossProfitMargin = if (netSales > 0.0) (grossProfit / netSales) * 100.0 else 0.0

        // 5. Ambil Biaya Operasional dan lakukan grouping per kategori
        val expenses = financeRepository.getExpensesBetween(startTime, endTime)
        val totalOperatingExpenses = expenses.sumOf { it.amount }
        val expenseBreakdown = ExpenseCategory.entries.associateWith { category ->
            expenses.filter { it.category == category }.sumOf { it.amount }
        }.filterValues { it > 0.0 }

        // 6. Hitung Laba Bersih (Net Profit) dan Margin Laba Bersih
        val netProfit = grossProfit - totalOperatingExpenses
        val netProfitMargin = if (netSales > 0.0) (netProfit / netSales) * 100.0 else 0.0

        // 7. Agregasi data statistik
        val totalOrdersCount = orders.size
        val totalItemsSold = orders.sumOf { it.items.sumOf { item -> item.quantity } }
        val averageOrderValue = if (totalOrdersCount > 0) netSales / totalOrdersCount else 0.0

        val topProducts = productStatMap.values.map { stat ->
            val productProfit = stat.revenue - stat.cogs
            val margin = if (stat.revenue > 0.0) (productProfit / stat.revenue) * 100.0 else 0.0
            ProductSalesMetric(
                productId = stat.productId,
                productName = stat.productName,
                quantitySold = stat.quantity,
                revenue = stat.revenue,
                cogs = stat.cogs,
                grossProfit = productProfit,
                marginPercent = margin
            )
        }.sortedByDescending { it.revenue }

        ProfitLossReport(
            startTime = startTime,
            endTime = endTime,
            grossSales = grossSales,
            totalDiscount = totalDiscount,
            totalRefund = totalRefund,
            netSales = netSales,
            totalCogs = totalCogs,
            grossProfit = grossProfit,
            grossProfitMargin = grossProfitMargin,
            operatingExpenses = expenses,
            totalOperatingExpenses = totalOperatingExpenses,
            expenseBreakdown = expenseBreakdown,
            netProfit = netProfit,
            netProfitMargin = netProfitMargin,
            totalOrdersCount = totalOrdersCount,
            totalItemsSold = totalItemsSold,
            averageOrderValue = averageOrderValue,
            topProducts = topProducts
        )
    }

    private class ProductStatAccumulator(
        val productId: String,
        val productName: String,
        var quantity: Int = 0,
        var revenue: Double = 0.0,
        var cogs: Double = 0.0
    )
}
