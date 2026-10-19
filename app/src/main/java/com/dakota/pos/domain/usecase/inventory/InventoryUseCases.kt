package com.dakota.pos.domain.usecase.inventory

import com.dakota.pos.domain.inventory.IdempotentStockDeductionEngine
import com.dakota.pos.domain.model.inventory.MovementType
import com.dakota.pos.domain.model.inventory.StockMovement
import com.dakota.pos.domain.model.inventory.StockOpname
import com.dakota.pos.domain.model.inventory.StockOpnameItem
import com.dakota.pos.domain.model.unit.UnitType
import com.dakota.pos.domain.recipe.UnitConverter
import com.dakota.pos.domain.repository.InventoryRepository
import java.util.UUID

/**
 * Use case untuk deduct stok otomatis saat order PAID.
 */
class DeductStockOnPaidUseCase(
    private val engine: IdempotentStockDeductionEngine
) {
    suspend operator fun invoke(
        orderId: String,
        lineItems: List<IdempotentStockDeductionEngine.OrderLineItem>,
        userId: String,
        userName: String
    ): IdempotentStockDeductionEngine.DeductionResult {
        return engine.deductStockForOrder(orderId, lineItems, userId, userName)
    }
}

/**
 * Use case untuk return stok saat order di-cancel/refund.
 */
class ReturnStockOnCancelUseCase(
    private val engine: IdempotentStockDeductionEngine
) {
    suspend operator fun invoke(
        orderId: String,
        userId: String,
        userName: String
    ): IdempotentStockDeductionEngine.DeductionResult {
        return engine.returnStockForOrder(orderId, userId, userName)
    }
}

/**
 * Use case untuk stok masuk / keluar manual.
 */
class ManualStockMovementUseCase(
    private val inventoryRepository: InventoryRepository
) {
    suspend fun stockIn(
        ingredientId: String,
        quantity: Double,
        notes: String?,
        userId: String,
        userName: String
    ) {
        val ingredient = inventoryRepository.getIngredientById(ingredientId)
            ?: throw IllegalArgumentException("Ingredient $ingredientId tidak ditemukan")

        val balanceBefore = ingredient.currentStock
        val balanceAfter = balanceBefore + quantity

        val movement = StockMovement(
            id = UUID.randomUUID().toString(),
            ingredientId = ingredient.id,
            ingredientName = ingredient.name,
            type = MovementType.IN,
            quantityChange = quantity,
            unit = ingredient.defaultUnit,
            balanceBefore = balanceBefore,
            balanceAfter = balanceAfter,
            notes = notes ?: "Stok masuk manual",
            userId = userId,
            userName = userName
        )

        inventoryRepository.recordMovement(movement)
        inventoryRepository.updateIngredientStock(ingredientId, balanceAfter)
    }

    suspend fun stockOut(
        ingredientId: String,
        quantity: Double,
        notes: String?,
        userId: String,
        userName: String
    ) {
        val ingredient = inventoryRepository.getIngredientById(ingredientId)
            ?: throw IllegalArgumentException("Ingredient $ingredientId tidak ditemukan")

        val balanceBefore = ingredient.currentStock
        val balanceAfter = balanceBefore - quantity

        val movement = StockMovement(
            id = UUID.randomUUID().toString(),
            ingredientId = ingredient.id,
            ingredientName = ingredient.name,
            type = MovementType.OUT,
            quantityChange = -quantity,
            unit = ingredient.defaultUnit,
            balanceBefore = balanceBefore,
            balanceAfter = balanceAfter,
            notes = notes ?: "Stok keluar manual",
            userId = userId,
            userName = userName
        )

        inventoryRepository.recordMovement(movement)
        inventoryRepository.updateIngredientStock(ingredientId, balanceAfter)
    }
}

/**
 * Use case untuk pencatatan wastage (bahan terbuang/tumpah/basi).
 */
class RecordWastageUseCase(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(
        ingredientId: String,
        quantity: Double,
        reason: String,
        userId: String,
        userName: String
    ) {
        val ingredient = inventoryRepository.getIngredientById(ingredientId)
            ?: throw IllegalArgumentException("Ingredient $ingredientId tidak ditemukan")

        val balanceBefore = ingredient.currentStock
        val balanceAfter = balanceBefore - quantity

        val movement = StockMovement(
            id = UUID.randomUUID().toString(),
            ingredientId = ingredient.id,
            ingredientName = ingredient.name,
            type = MovementType.WASTAGE,
            quantityChange = -quantity,
            unit = ingredient.defaultUnit,
            balanceBefore = balanceBefore,
            balanceAfter = balanceAfter,
            notes = "Wastage: $reason",
            userId = userId,
            userName = userName
        )

        inventoryRepository.recordMovement(movement)
        inventoryRepository.updateIngredientStock(ingredientId, balanceAfter)
    }
}

/**
 * Use case untuk melaksanakan Stock Opname dan adjustment.
 */
class PerformStockOpnameUseCase(
    private val inventoryRepository: InventoryRepository
) {
    /**
     * @param physicalCounts Map<ingredientId, physicalStockCount>
     */
    suspend operator fun invoke(
        physicalCounts: Map<String, Double>,
        userId: String,
        userName: String,
        notes: String? = null
    ): StockOpname {
        val opnameId = UUID.randomUUID().toString()
        val opnameNumber = "SO-${System.currentTimeMillis()}"
        val now = System.currentTimeMillis()

        val items = mutableListOf<StockOpnameItem>()
        val adjustmentMovements = mutableListOf<StockMovement>()

        for ((ingredientId, physicalStock) in physicalCounts) {
            val ingredient = inventoryRepository.getIngredientById(ingredientId) ?: continue
            val systemStock = ingredient.currentStock
            val difference = physicalStock - systemStock

            items.add(
                StockOpnameItem(
                    id = UUID.randomUUID().toString(),
                    opnameId = opnameId,
                    ingredientId = ingredient.id,
                    ingredientName = ingredient.name,
                    systemStock = systemStock,
                    physicalStock = physicalStock,
                    difference = difference,
                    unit = ingredient.defaultUnit,
                    notes = null
                )
            )

            // Buat adjustment movement jika ada selisih
            if (difference != 0.0) {
                adjustmentMovements.add(
                    StockMovement(
                        id = UUID.randomUUID().toString(),
                        ingredientId = ingredient.id,
                        ingredientName = ingredient.name,
                        type = MovementType.ADJUSTMENT,
                        quantityChange = difference,
                        unit = ingredient.defaultUnit,
                        balanceBefore = systemStock,
                        balanceAfter = physicalStock,
                        referenceId = opnameId,
                        notes = "Stock Opname $opnameNumber - Selisih: $difference ${ingredient.defaultUnit.symbol}",
                        userId = userId,
                        userName = userName,
                        timestamp = now
                    )
                )

                // Update stok ke nilai fisik
                inventoryRepository.updateIngredientStock(ingredientId, physicalStock)
            }
        }

        // Record opname
        val opname = StockOpname(
            id = opnameId,
            opnameNumber = opnameNumber,
            performedByUserId = userId,
            performedByUserName = userName,
            items = items,
            notes = notes,
            timestamp = now
        )
        inventoryRepository.saveStockOpname(opname)

        // Record adjustment movements
        if (adjustmentMovements.isNotEmpty()) {
            inventoryRepository.recordMovements(adjustmentMovements)
        }

        return opname
    }
}

/**
 * Use case untuk mengambil data monitoring stok dan log mutasi.
 */
class GetInventoryDataUseCase(
    private val inventoryRepository: InventoryRepository
) {
    fun getIngredientsFlow() = inventoryRepository.getAllIngredientsFlow()
    fun getRecentMovements(limit: Int = 100) = inventoryRepository.getRecentMovements(limit)
    fun getMovementsForIngredient(ingredientId: String) = inventoryRepository.getMovementsForIngredient(ingredientId)
    fun getAllOpnamesFlow() = inventoryRepository.getAllOpnamesFlow()
}
