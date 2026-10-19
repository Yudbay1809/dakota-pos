package com.dakota.pos.domain.inventory

data class IngredientDeductionItem(
    val ingredientId: String,
    val ingredientName: String,
    val quantityToDeduct: Double,
    val unit: String
)

sealed class DeductionResult {
    data class Success(
        val transactionId: String,
        val deductedItems: List<IngredientDeductionItem>
    ) : DeductionResult()

    data class AlreadyProcessed(
        val transactionId: String,
        val message: String = "Transaction already processed (Idempotent Guard)"
    ) : DeductionResult()

    data class InsufficientStock(
        val ingredientId: String,
        val ingredientName: String,
        val currentStock: Double,
        val requiredStock: Double
    ) : DeductionResult()

    data class Failure(
        val transactionId: String,
        val reason: String
    ) : DeductionResult()
}

/**
 * Abstraksi mesin pengurangan inventori berbasis resep.
 * Wajib menerapkan prinsip Idempotency: transaksi yang sama tidak boleh
 * memotong stok dua kali jika dipicu ulang.
 */
interface InventoryDeductionEngine {
    suspend fun deductStockForPaidOrder(
        orderId: String,
        transactionId: String,
        items: List<IngredientDeductionItem>
    ): DeductionResult

    suspend fun reverseStockDeduction(
        orderId: String,
        transactionId: String,
        reason: String
    ): Boolean
}
