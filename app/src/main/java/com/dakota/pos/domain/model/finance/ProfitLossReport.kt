package com.dakota.pos.domain.model.finance

data class ProfitLossReport(
    val startTime: Long,
    val endTime: Long,
    val grossSales: Double,
    val totalDiscount: Double,
    val totalRefund: Double,
    val netSales: Double,
    val totalCogs: Double,
    val grossProfit: Double,
    val grossProfitMargin: Double,
    val operatingExpenses: List<OperatingExpense> = emptyList(),
    val totalOperatingExpenses: Double,
    val expenseBreakdown: Map<ExpenseCategory, Double> = emptyMap(),
    val netProfit: Double,
    val netProfitMargin: Double,
    val totalOrdersCount: Int,
    val totalItemsSold: Int,
    val averageOrderValue: Double,
    val topProducts: List<ProductSalesMetric> = emptyList()
) {
    val isProfitable: Boolean
        get() = netProfit >= 0.0
}
