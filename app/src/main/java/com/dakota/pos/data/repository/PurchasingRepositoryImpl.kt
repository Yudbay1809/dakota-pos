package com.dakota.pos.data.repository

import com.dakota.pos.data.local.dao.GoodsReceiptDao
import com.dakota.pos.data.local.dao.PurchaseOrderDao
import com.dakota.pos.data.local.dao.SupplierDao
import com.dakota.pos.data.local.entity.GoodsReceiptEntity
import com.dakota.pos.data.local.entity.GoodsReceiptItemEntity
import com.dakota.pos.data.local.entity.PurchaseOrderEntity
import com.dakota.pos.data.local.entity.PurchaseOrderItemEntity
import com.dakota.pos.data.local.entity.SupplierEntity
import com.dakota.pos.domain.model.purchasing.GoodsReceipt
import com.dakota.pos.domain.model.purchasing.GoodsReceiptItem
import com.dakota.pos.domain.model.purchasing.PurchaseOrder
import com.dakota.pos.domain.model.purchasing.PurchaseOrderItem
import com.dakota.pos.domain.model.purchasing.PurchaseOrderStatus
import com.dakota.pos.domain.model.purchasing.Supplier
import com.dakota.pos.domain.model.unit.UnitType
import com.dakota.pos.domain.repository.PurchasingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PurchasingRepositoryImpl(
    private val supplierDao: SupplierDao,
    private val purchaseOrderDao: PurchaseOrderDao,
    private val goodsReceiptDao: GoodsReceiptDao
) : PurchasingRepository {

    // ========================================================================
    // SUPPLIERS
    // ========================================================================

    override fun getAllSuppliersFlow(): Flow<List<Supplier>> {
        return supplierDao.getAllSuppliersFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getActiveSuppliersFlow(): Flow<List<Supplier>> {
        return supplierDao.getActiveSuppliersFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getSupplierById(id: String): Supplier? {
        return supplierDao.getSupplierById(id)?.toDomain()
    }

    override suspend fun saveSupplier(supplier: Supplier) {
        supplierDao.insertSupplier(supplier.toEntity())
    }

    override suspend fun deleteSupplier(id: String) {
        supplierDao.deleteSupplier(id)
    }

    override suspend fun seedInitialSuppliersIfEmpty() {
        if (supplierDao.getSupplierCount() == 0) {
            val initial = listOf(
                Supplier(
                    id = "sup_coffee_01",
                    name = "PT Sukses Kopi Nusantara",
                    contactPerson = "Budi Hartono",
                    phone = "0812-3456-7890",
                    email = "order@kopinusantara.co.id",
                    address = "Kawasan Industri Pulo Gadung, Jakarta Timur",
                    paymentTerms = "NET_14"
                ),
                Supplier(
                    id = "sup_dairy_02",
                    name = "CV Dairy Prima Fresh",
                    contactPerson = "Siti Rahma",
                    phone = "0813-9876-5432",
                    email = "sales@dairyprima.com",
                    address = "Jl. Raya Bogor KM 28, Cimanggis, Depok",
                    paymentTerms = "NET_7"
                ),
                Supplier(
                    id = "sup_sweet_03",
                    name = "PT Manis Alami Sentosa",
                    contactPerson = "Hendro Wibowo",
                    phone = "0811-2233-4455",
                    email = "order@manisalami.id",
                    address = "Jl. Daan Mogot KM 12, Jakarta Barat",
                    paymentTerms = "CASH"
                )
            )
            initial.forEach { supplierDao.insertSupplier(it.toEntity()) }
        }
    }

    // ========================================================================
    // PURCHASE ORDERS
    // ========================================================================

    override fun getAllPurchaseOrdersFlow(): Flow<List<PurchaseOrder>> {
        return purchaseOrderDao.getAllPurchaseOrdersFlow().map { list ->
            list.map { entity ->
                val items = purchaseOrderDao.getItemsForPurchaseOrder(entity.id)
                entity.toDomain(items.map { it.toDomain() })
            }
        }
    }

    override fun getPurchaseOrdersByStatusFlow(status: PurchaseOrderStatus): Flow<List<PurchaseOrder>> {
        return purchaseOrderDao.getPurchaseOrdersByStatusFlow(status.name).map { list ->
            list.map { entity ->
                val items = purchaseOrderDao.getItemsForPurchaseOrder(entity.id)
                entity.toDomain(items.map { it.toDomain() })
            }
        }
    }

    override suspend fun getPurchaseOrderById(id: String): PurchaseOrder? {
        val entity = purchaseOrderDao.getPurchaseOrderById(id) ?: return null
        val items = purchaseOrderDao.getItemsForPurchaseOrder(id)
        return entity.toDomain(items.map { it.toDomain() })
    }

    override suspend fun savePurchaseOrder(order: PurchaseOrder) {
        val orderEntity = order.toEntity()
        val itemEntities = order.items.map { it.toEntity() }
        purchaseOrderDao.saveFullPurchaseOrder(orderEntity, itemEntities)
    }

    override suspend fun updateOrderStatus(id: String, status: PurchaseOrderStatus) {
        purchaseOrderDao.updateOrderStatus(id, status.name)
    }

    override suspend fun updateItemReceivedQuantity(itemId: String, receivedQuantity: Double) {
        purchaseOrderDao.updateItemReceivedQuantity(itemId, receivedQuantity)
    }

    override suspend fun generateNextPoNumber(): String {
        val count = purchaseOrderDao.getPurchaseOrderCount() + 1
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        return "PO-$dateStr-%03d".format(count)
    }

    // ========================================================================
    // GOODS RECEIPTS
    // ========================================================================

    override fun getAllGoodsReceiptsFlow(): Flow<List<GoodsReceipt>> {
        return goodsReceiptDao.getAllGoodsReceiptsFlow().map { list ->
            list.map { entity ->
                val items = goodsReceiptDao.getItemsForReceipt(entity.id)
                entity.toDomain(items.map { it.toDomain() })
            }
        }
    }

    override fun getReceiptsForOrderFlow(orderId: String): Flow<List<GoodsReceipt>> {
        return goodsReceiptDao.getReceiptsForOrderFlow(orderId).map { list ->
            list.map { entity ->
                val items = goodsReceiptDao.getItemsForReceipt(entity.id)
                entity.toDomain(items.map { it.toDomain() })
            }
        }
    }

    override suspend fun saveGoodsReceipt(receipt: GoodsReceipt) {
        val receiptEntity = receipt.toEntity()
        val itemEntities = receipt.items.map { it.toEntity() }
        goodsReceiptDao.saveFullGoodsReceipt(receiptEntity, itemEntities)
    }

    override suspend fun generateNextReceiptNumber(): String {
        val count = goodsReceiptDao.getGoodsReceiptCount() + 1
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        return "GR-$dateStr-%03d".format(count)
    }

    // ========================================================================
    // MAPPERS
    // ========================================================================

    private fun SupplierEntity.toDomain() = Supplier(
        id = id,
        name = name,
        contactPerson = contactPerson,
        phone = phone,
        email = email,
        address = address,
        paymentTerms = paymentTerms,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Supplier.toEntity() = SupplierEntity(
        id = id,
        name = name,
        contactPerson = contactPerson,
        phone = phone,
        email = email,
        address = address,
        paymentTerms = paymentTerms,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun PurchaseOrderEntity.toDomain(items: List<PurchaseOrderItem>) = PurchaseOrder(
        id = id,
        poNumber = poNumber,
        supplierId = supplierId,
        supplierName = supplierName,
        orderDate = orderDate,
        expectedDeliveryDate = expectedDeliveryDate,
        status = runCatching { PurchaseOrderStatus.valueOf(status) }.getOrDefault(PurchaseOrderStatus.DRAFT),
        totalAmount = totalAmount,
        notes = notes,
        items = items,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun PurchaseOrder.toEntity() = PurchaseOrderEntity(
        id = id,
        poNumber = poNumber,
        supplierId = supplierId,
        supplierName = supplierName,
        orderDate = orderDate,
        expectedDeliveryDate = expectedDeliveryDate,
        status = status.name,
        totalAmount = totalAmount,
        notes = notes,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun PurchaseOrderItemEntity.toDomain() = PurchaseOrderItem(
        id = id,
        purchaseOrderId = purchaseOrderId,
        ingredientId = ingredientId,
        ingredientName = ingredientName,
        unit = UnitType.fromString(unit),
        orderedQuantity = orderedQuantity,
        unitPrice = unitPrice,
        subtotal = subtotal,
        receivedQuantity = receivedQuantity
    )

    private fun PurchaseOrderItem.toEntity() = PurchaseOrderItemEntity(
        id = id,
        purchaseOrderId = purchaseOrderId,
        ingredientId = ingredientId,
        ingredientName = ingredientName,
        unit = unit.symbol,
        orderedQuantity = orderedQuantity,
        unitPrice = unitPrice,
        subtotal = subtotal,
        receivedQuantity = receivedQuantity
    )

    private fun GoodsReceiptEntity.toDomain(items: List<GoodsReceiptItem>) = GoodsReceipt(
        id = id,
        receiptNumber = receiptNumber,
        purchaseOrderId = purchaseOrderId,
        poNumber = poNumber,
        supplierName = supplierName,
        receivedDate = receivedDate,
        receivedBy = receivedBy,
        notes = notes,
        items = items,
        timestamp = timestamp
    )

    private fun GoodsReceipt.toEntity() = GoodsReceiptEntity(
        id = id,
        receiptNumber = receiptNumber,
        purchaseOrderId = purchaseOrderId,
        poNumber = poNumber,
        supplierName = supplierName,
        receivedDate = receivedDate,
        receivedBy = receivedBy,
        notes = notes,
        timestamp = timestamp
    )

    private fun GoodsReceiptItemEntity.toDomain() = GoodsReceiptItem(
        id = id,
        goodsReceiptId = goodsReceiptId,
        purchaseOrderItemId = purchaseOrderItemId,
        ingredientId = ingredientId,
        ingredientName = ingredientName,
        unit = UnitType.fromString(unit),
        receivedQuantity = receivedQuantity,
        unitPrice = unitPrice,
        notes = notes
    )

    private fun GoodsReceiptItem.toEntity() = GoodsReceiptItemEntity(
        id = id,
        goodsReceiptId = goodsReceiptId,
        purchaseOrderItemId = purchaseOrderItemId,
        ingredientId = ingredientId,
        ingredientName = ingredientName,
        unit = unit.symbol,
        receivedQuantity = receivedQuantity,
        unitPrice = unitPrice,
        notes = notes
    )
}
