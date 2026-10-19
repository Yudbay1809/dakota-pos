package com.dakota.pos.domain.repository

import com.dakota.pos.domain.model.purchasing.GoodsReceipt
import com.dakota.pos.domain.model.purchasing.PurchaseOrder
import com.dakota.pos.domain.model.purchasing.PurchaseOrderStatus
import com.dakota.pos.domain.model.purchasing.Supplier
import kotlinx.coroutines.flow.Flow

interface PurchasingRepository {

    // Suppliers
    fun getAllSuppliersFlow(): Flow<List<Supplier>>
    fun getActiveSuppliersFlow(): Flow<List<Supplier>>
    suspend fun getSupplierById(id: String): Supplier?
    suspend fun saveSupplier(supplier: Supplier)
    suspend fun deleteSupplier(id: String)
    suspend fun seedInitialSuppliersIfEmpty()

    // Purchase Orders
    fun getAllPurchaseOrdersFlow(): Flow<List<PurchaseOrder>>
    fun getPurchaseOrdersByStatusFlow(status: PurchaseOrderStatus): Flow<List<PurchaseOrder>>
    suspend fun getPurchaseOrderById(id: String): PurchaseOrder?
    suspend fun savePurchaseOrder(order: PurchaseOrder)
    suspend fun updateOrderStatus(id: String, status: PurchaseOrderStatus)
    suspend fun updateItemReceivedQuantity(itemId: String, receivedQuantity: Double)
    suspend fun generateNextPoNumber(): String

    // Goods Receipts
    fun getAllGoodsReceiptsFlow(): Flow<List<GoodsReceipt>>
    fun getReceiptsForOrderFlow(orderId: String): Flow<List<GoodsReceipt>>
    suspend fun saveGoodsReceipt(receipt: GoodsReceipt)
    suspend fun generateNextReceiptNumber(): String
}
