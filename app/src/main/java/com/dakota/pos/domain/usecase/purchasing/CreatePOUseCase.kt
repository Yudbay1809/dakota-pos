package com.dakota.pos.domain.usecase.purchasing

import com.dakota.pos.domain.model.purchasing.PurchaseOrder
import com.dakota.pos.domain.model.purchasing.PurchaseOrderItem
import com.dakota.pos.domain.model.purchasing.PurchaseOrderStatus
import com.dakota.pos.domain.repository.PurchasingRepository
import java.util.UUID

class CreatePOUseCase(
    private val purchasingRepository: PurchasingRepository
) {

    suspend operator fun invoke(
        supplierId: String,
        items: List<PurchaseOrderItem>,
        expectedDeliveryDate: Long? = null,
        notes: String? = null,
        createdBy: String,
        submitImmediately: Boolean = false
    ): Result<PurchaseOrder> {
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("Item pembelian tidak boleh kosong"))
        }

        val supplier = purchasingRepository.getSupplierById(supplierId)
            ?: return Result.failure(IllegalArgumentException("Supplier dengan ID $supplierId tidak ditemukan"))

        for (item in items) {
            if (item.orderedQuantity <= 0.0) {
                return Result.failure(IllegalArgumentException("Kuantitas untuk item '${item.ingredientName}' harus lebih besar dari 0"))
            }
            if (item.unitPrice < 0.0) {
                return Result.failure(IllegalArgumentException("Harga satuan untuk item '${item.ingredientName}' tidak boleh negatif"))
            }
        }

        val poId = UUID.randomUUID().toString()
        val poNumber = purchasingRepository.generateNextPoNumber()
        val totalAmount = items.sumOf { it.orderedQuantity * it.unitPrice }

        val assignedItems = items.map { item ->
            item.copy(
                id = if (item.id.isBlank()) UUID.randomUUID().toString() else item.id,
                purchaseOrderId = poId,
                subtotal = item.orderedQuantity * item.unitPrice,
                receivedQuantity = 0.0
            )
        }

        val status = if (submitImmediately) PurchaseOrderStatus.SUBMITTED else PurchaseOrderStatus.DRAFT

        val purchaseOrder = PurchaseOrder(
            id = poId,
            poNumber = poNumber,
            supplierId = supplier.id,
            supplierName = supplier.name,
            orderDate = System.currentTimeMillis(),
            expectedDeliveryDate = expectedDeliveryDate,
            status = status,
            totalAmount = totalAmount,
            notes = notes,
            items = assignedItems,
            createdBy = createdBy,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        purchasingRepository.savePurchaseOrder(purchaseOrder)

        return Result.success(purchaseOrder)
    }
}
