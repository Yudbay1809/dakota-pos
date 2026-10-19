package com.dakota.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.dakota.pos.data.local.entity.GoodsReceiptEntity
import com.dakota.pos.data.local.entity.GoodsReceiptItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoodsReceiptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoodsReceipt(receipt: GoodsReceiptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoodsReceiptItems(items: List<GoodsReceiptItemEntity>)

    @Query("SELECT * FROM goods_receipts ORDER BY timestamp DESC")
    fun getAllGoodsReceiptsFlow(): Flow<List<GoodsReceiptEntity>>

    @Query("SELECT * FROM goods_receipts WHERE purchaseOrderId = :orderId ORDER BY timestamp DESC")
    fun getReceiptsForOrderFlow(orderId: String): Flow<List<GoodsReceiptEntity>>

    @Query("SELECT * FROM goods_receipt_items WHERE goodsReceiptId = :receiptId")
    suspend fun getItemsForReceipt(receiptId: String): List<GoodsReceiptItemEntity>

    @Transaction
    suspend fun saveFullGoodsReceipt(receipt: GoodsReceiptEntity, items: List<GoodsReceiptItemEntity>) {
        insertGoodsReceipt(receipt)
        insertGoodsReceiptItems(items)
    }

    @Query("SELECT COUNT(*) FROM goods_receipts")
    suspend fun getGoodsReceiptCount(): Int
}
