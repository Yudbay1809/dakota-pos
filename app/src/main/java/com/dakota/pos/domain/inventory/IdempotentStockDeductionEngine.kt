package com.dakota.pos.domain.inventory

import com.dakota.pos.domain.model.inventory.MovementType
import com.dakota.pos.domain.model.inventory.StockMovement
import com.dakota.pos.domain.model.unit.UnitType
import com.dakota.pos.domain.recipe.UnitConverter
import com.dakota.pos.domain.repository.InventoryRepository
import com.dakota.pos.domain.repository.RecipeRepository
import java.util.UUID

/**
 * Engine pengurangan stok otomatis saat order dibayar.
 *
 * PRINSIP IDEMPOTENT:
 * - Setiap orderId hanya diproses SEKALI.
 * - Jika orderId sudah tercatat di stock_movements (type=SALE), engine SKIP tanpa error.
 * - Ini mencegah double-cut akibat retry, sinkronisasi ulang, atau trigger ganda.
 *
 * FLOW:
 * PAID ORDER → Ambil OrderItems → Untuk setiap item, ambil Recipe →
 * Breakdown RecipeItems → Konversi unit ke defaultUnit ingredient →
 * Kalkulasi quantity * orderItem.quantity → Buat StockMovement (SALE) →
 * Update ingredient currentStock
 */
class IdempotentStockDeductionEngine(
    private val inventoryRepository: InventoryRepository,
    private val recipeRepository: RecipeRepository
) {

    /**
     * Hasil deduction untuk satu order.
     */
    data class DeductionResult(
        val orderId: String,
        val wasAlreadyProcessed: Boolean,
        val movements: List<StockMovement>,
        val insufficientStockWarnings: List<InsufficientStockWarning>
    )

    data class InsufficientStockWarning(
        val ingredientId: String,
        val ingredientName: String,
        val requiredAmount: Double,
        val availableStock: Double,
        val unit: UnitType
    )

    /**
     * Data item order yang perlu dideduct stoknya.
     */
    data class OrderLineItem(
        val productId: String,
        val variantId: String?,
        val quantity: Int
    )

    /**
     * Memproses pengurangan stok untuk order yang sudah PAID.
     * IDEMPOTENT: Aman dipanggil berkali-kali untuk orderId yang sama.
     *
     * @param orderId ID order yang statusnya PAID
     * @param lineItems Daftar item dalam order
     * @param userId ID user yang memproses pembayaran
     * @param userName Nama user yang memproses
     * @return DeductionResult berisi movements yang dibuat & warning jika stok minus
     */
    suspend fun deductStockForOrder(
        orderId: String,
        lineItems: List<OrderLineItem>,
        userId: String,
        userName: String
    ): DeductionResult {
        // ── IDEMPOTENCY CHECK ──────────────────────────────────────
        if (inventoryRepository.hasBeenDeductedForOrder(orderId)) {
            return DeductionResult(
                orderId = orderId,
                wasAlreadyProcessed = true,
                movements = emptyList(),
                insufficientStockWarnings = emptyList()
            )
        }

        // ── AGGREGATE INGREDIENT REQUIREMENTS ──────────────────────
        // Kumpulkan semua kebutuhan ingredient dari semua line items
        val ingredientRequirements = mutableMapOf<String, IngredientRequirement>()

        for (lineItem in lineItems) {
            val recipe = recipeRepository.getRecipeForProduct(lineItem.productId, lineItem.variantId)
                ?: recipeRepository.getRecipeForProduct(lineItem.productId, null)
                ?: continue // No recipe = skip (produk tanpa resep)

            for (recipeItem in recipe.items) {
                val ingredient = inventoryRepository.getIngredientById(recipeItem.ingredientId)
                    ?: continue

                // Konversi unit resep ke unit default ingredient
                val quantityInDefaultUnit = try {
                    UnitConverter.convert(recipeItem.quantity, recipeItem.unit, ingredient.defaultUnit)
                } catch (e: Exception) {
                    recipeItem.quantity // Fallback jika unit incompatible
                }

                val totalRequired = quantityInDefaultUnit * lineItem.quantity

                val existing = ingredientRequirements[recipeItem.ingredientId]
                if (existing != null) {
                    ingredientRequirements[recipeItem.ingredientId] = existing.copy(
                        totalRequired = existing.totalRequired + totalRequired
                    )
                } else {
                    ingredientRequirements[recipeItem.ingredientId] = IngredientRequirement(
                        ingredientId = ingredient.id,
                        ingredientName = ingredient.name,
                        totalRequired = totalRequired,
                        currentStock = ingredient.currentStock,
                        unit = ingredient.defaultUnit
                    )
                }
            }
        }

        // ── CREATE MOVEMENTS & DEDUCT ──────────────────────────────
        val movements = mutableListOf<StockMovement>()
        val warnings = mutableListOf<InsufficientStockWarning>()
        val now = System.currentTimeMillis()

        for ((_, req) in ingredientRequirements) {
            val balanceBefore = req.currentStock
            val balanceAfter = balanceBefore - req.totalRequired

            // Warning jika stok jadi negatif (tetap diproses, hanya warning)
            if (balanceAfter < 0) {
                warnings.add(
                    InsufficientStockWarning(
                        ingredientId = req.ingredientId,
                        ingredientName = req.ingredientName,
                        requiredAmount = req.totalRequired,
                        availableStock = req.currentStock,
                        unit = req.unit
                    )
                )
            }

            val movement = StockMovement(
                id = UUID.randomUUID().toString(),
                ingredientId = req.ingredientId,
                ingredientName = req.ingredientName,
                type = MovementType.SALE,
                quantityChange = -req.totalRequired,
                unit = req.unit,
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
                referenceId = orderId,
                notes = "Auto deduction - Order $orderId",
                userId = userId,
                userName = userName,
                timestamp = now
            )
            movements.add(movement)

            // Update stok ingredient
            inventoryRepository.updateIngredientStock(req.ingredientId, balanceAfter)
        }

        // Record semua movements ke ledger
        if (movements.isNotEmpty()) {
            inventoryRepository.recordMovements(movements)
        }

        return DeductionResult(
            orderId = orderId,
            wasAlreadyProcessed = false,
            movements = movements,
            insufficientStockWarnings = warnings
        )
    }

    /**
     * Mengembalikan stok yang sudah terpotong akibat pembatalan/refund order.
     * Juga IDEMPOTENT: Cek apakah RETURN sudah pernah dibuat untuk orderId ini.
     */
    suspend fun returnStockForOrder(
        orderId: String,
        userId: String,
        userName: String
    ): DeductionResult {
        // Ambil movements SALE terkait order ini
        val saleMovements = inventoryRepository.getMovementsByReferenceId(orderId)
            .filter { it.type == MovementType.SALE }

        if (saleMovements.isEmpty()) {
            return DeductionResult(
                orderId = orderId,
                wasAlreadyProcessed = true,
                movements = emptyList(),
                insufficientStockWarnings = emptyList()
            )
        }

        // Cek apakah RETURN sudah pernah dilakukan
        val existingReturns = inventoryRepository.getMovementsByReferenceId(orderId)
            .filter { it.type == MovementType.RETURN }
        if (existingReturns.isNotEmpty()) {
            return DeductionResult(
                orderId = orderId,
                wasAlreadyProcessed = true,
                movements = emptyList(),
                insufficientStockWarnings = emptyList()
            )
        }

        val returnMovements = mutableListOf<StockMovement>()
        val now = System.currentTimeMillis()

        for (saleMovement in saleMovements) {
            val ingredient = inventoryRepository.getIngredientById(saleMovement.ingredientId) ?: continue
            val returnAmount = -saleMovement.quantityChange // Reverse the negative
            val balanceBefore = ingredient.currentStock
            val balanceAfter = balanceBefore + returnAmount

            val returnMovement = StockMovement(
                id = UUID.randomUUID().toString(),
                ingredientId = saleMovement.ingredientId,
                ingredientName = saleMovement.ingredientName,
                type = MovementType.RETURN,
                quantityChange = returnAmount,
                unit = saleMovement.unit,
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
                referenceId = orderId,
                notes = "Return - Order $orderId (cancel/refund)",
                userId = userId,
                userName = userName,
                timestamp = now
            )
            returnMovements.add(returnMovement)

            inventoryRepository.updateIngredientStock(saleMovement.ingredientId, balanceAfter)
        }

        if (returnMovements.isNotEmpty()) {
            inventoryRepository.recordMovements(returnMovements)
        }

        return DeductionResult(
            orderId = orderId,
            wasAlreadyProcessed = false,
            movements = returnMovements,
            insufficientStockWarnings = emptyList()
        )
    }

    private data class IngredientRequirement(
        val ingredientId: String,
        val ingredientName: String,
        val totalRequired: Double,
        val currentStock: Double,
        val unit: UnitType
    )
}
