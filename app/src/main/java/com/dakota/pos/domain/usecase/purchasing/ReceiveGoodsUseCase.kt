package com.dakota.pos.domain.usecase.purchasing

import com.dakota.pos.domain.model.inventory.MovementType
import com.dakota.pos.domain.model.inventory.StockMovement
import com.dakota.pos.domain.model.purchasing.GoodsReceipt
import com.dakota.pos.domain.model.purchasing.GoodsReceiptItem
import com.dakota.pos.domain.model.purchasing.PurchaseOrderStatus
import com.dakota.pos.domain.model.unit.UnitType
import com.dakota.pos.domain.recipe.UnitConverter
import com.dakota.pos.domain.repository.InventoryRepository
import com.dakota.pos.domain.repository.PurchasingRepository
import com.dakota.pos.domain.repository.RecipeRepository
import java.util.UUID

data class IncomingItemInput(
    val purchaseOrderItemId: String,
    val quantityReceived: Double,
    val notes: String? = null
)

class ReceiveGoodsUseCase(
    private val purchasingRepository: PurchasingRepository,
    private val inventoryRepository: InventoryRepository,
    private val recipeRepository: RecipeRepository,
    private val updateIngredientCostUseCase: UpdateIngredientCostUseCase
) {

    suspend operator fun invoke(
        purchaseOrderId: String,
        receivedBy: String,
        receivedItems: List<IncomingItemInput>,
        receiptNotes: String? = null
    ): Result<GoodsReceipt> {
        val po = purchasingRepository.getPurchaseOrderById(purchaseOrderId)
            ?: return Result.failure(IllegalArgumentException("Purchase Order dengan ID $purchaseOrderId tidak ditemukan"))

        if (po.status == PurchaseOrderStatus.COMPLETED) {
            return Result.failure(IllegalStateException("Purchase Order ${po.poNumber} sudah selesai (COMPLETED) dan tidak dapat menerima barang lagi"))
        }

        if (po.status == PurchaseOrderStatus.CANCELLED) {
            return Result.failure(IllegalStateException("Purchase Order ${po.poNumber} telah dibatalkan (CANCELLED)"))
        }

        if (po.status == PurchaseOrderStatus.DRAFT) {
            return Result.failure(IllegalStateException("Purchase Order ${po.poNumber} masih berstatus DRAFT. Harap Submit PO terlebih dahulu sebelum penerimaan barang"))
        }

        val validIncoming = receivedItems.filter { it.quantityReceived > 0.0 }
        if (validIncoming.isEmpty()) {
            return Result.failure(IllegalArgumentException("Tidak ada kuantitas barang yang diterima (> 0)"))
        }

        val receiptId = UUID.randomUUID().toString()
        val receiptNumber = purchasingRepository.generateNextReceiptNumber()
        val receiptTimestamp = System.currentTimeMillis()

        val goodsReceiptItems = mutableListOf<GoodsReceiptItem>()
        val updatedPoItems = po.items.toMutableList()

        for (incoming in validIncoming) {
            val itemIndex = updatedPoItems.indexOfFirst { it.id == incoming.purchaseOrderItemId }
            if (itemIndex == -1) {
                return Result.failure(IllegalArgumentException("Item PO dengan ID ${incoming.purchaseOrderItemId} tidak ditemukan di PO ${po.poNumber}"))
            }

            val poItem = updatedPoItems[itemIndex]
            val newTotalReceived = poItem.receivedQuantity + incoming.quantityReceived

            // Perbarui item pada PO
            updatedPoItems[itemIndex] = poItem.copy(receivedQuantity = newTotalReceived)
            purchasingRepository.updateItemReceivedQuantity(poItem.id, newTotalReceived)

            // Buat GoodsReceiptItem
            val grItem = GoodsReceiptItem(
                id = UUID.randomUUID().toString(),
                goodsReceiptId = receiptId,
                purchaseOrderItemId = poItem.id,
                ingredientId = poItem.ingredientId,
                ingredientName = poItem.ingredientName,
                unit = poItem.unit,
                receivedQuantity = incoming.quantityReceived,
                unitPrice = poItem.unitPrice,
                notes = incoming.notes
            )
            goodsReceiptItems.add(grItem)

            // Update Stok Bahan Baku & Hitung HPP Baru
            val ingredient = recipeRepository.getIngredientById(poItem.ingredientId)
            if (ingredient != null) {
                val convertedQty = if (UnitConverter.areCompatible(poItem.unit, ingredient.defaultUnit)) {
                    UnitConverter.convert(incoming.quantityReceived, poItem.unit, ingredient.defaultUnit)
                } else {
                    incoming.quantityReceived
                }

                val currentStock = ingredient.currentStock
                val newStock = currentStock + convertedQty

                // Update moving average cost
                updateIngredientCostUseCase(
                    ingredientId = ingredient.id,
                    receivedQuantity = incoming.quantityReceived,
                    receivedUnit = poItem.unit,
                    receivedUnitPrice = poItem.unitPrice
                )

                // Update stock di database
                inventoryRepository.updateIngredientStock(ingredient.id, newStock)

                // Catat mutasi stok IN
                val movement = StockMovement(
                    id = UUID.randomUUID().toString(),
                    ingredientId = ingredient.id,
                    ingredientName = ingredient.name,
                    type = MovementType.IN,
                    quantityChange = convertedQty,
                    unit = ingredient.defaultUnit,
                    balanceBefore = currentStock,
                    balanceAfter = newStock,
                    referenceId = receiptNumber,
                    notes = "Penerimaan PO ${po.poNumber} ($receiptNumber)",
                    userId = receivedBy,
                    userName = receivedBy,
                    timestamp = receiptTimestamp
                )
                inventoryRepository.recordMovement(movement)
            }
        }

        // Tentukan status akhir PO: apakah COMPLETED atau PARTIAL (RECEIVED)
        val allCompleted = updatedPoItems.all { it.isFullyReceived }
        val newStatus = if (allCompleted) PurchaseOrderStatus.COMPLETED else PurchaseOrderStatus.RECEIVED

        purchasingRepository.updateOrderStatus(po.id, newStatus)

        // Simpan GoodsReceipt
        val goodsReceipt = GoodsReceipt(
            id = receiptId,
            receiptNumber = receiptNumber,
            purchaseOrderId = po.id,
            poNumber = po.poNumber,
            supplierName = po.supplierName,
            receivedDate = receiptTimestamp,
            receivedBy = receivedBy,
            notes = receiptNotes,
            items = goodsReceiptItems,
            timestamp = receiptTimestamp
        )
        purchasingRepository.saveGoodsReceipt(goodsReceipt)

        return Result.success(goodsReceipt)
    }
}
