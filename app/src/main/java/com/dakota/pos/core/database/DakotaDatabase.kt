package com.dakota.pos.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dakota.pos.data.local.dao.AuditLogDao
import com.dakota.pos.data.local.dao.CategoryDao
import com.dakota.pos.data.local.dao.IngredientDao
import com.dakota.pos.data.local.dao.OrderDao
import com.dakota.pos.data.local.dao.ProductDao
import com.dakota.pos.data.local.dao.RecipeDao
import com.dakota.pos.data.local.dao.StockMovementDao
import com.dakota.pos.data.local.dao.StockOpnameDao
import com.dakota.pos.data.local.dao.UserDao
import com.dakota.pos.data.local.entity.AuditLogEntity
import com.dakota.pos.data.local.entity.CategoryEntity
import com.dakota.pos.data.local.entity.IngredientEntity
import com.dakota.pos.data.local.entity.ModifierEntity
import com.dakota.pos.data.local.entity.OrderEntity
import com.dakota.pos.data.local.entity.OrderItemEntity
import com.dakota.pos.data.local.entity.ProductEntity
import com.dakota.pos.data.local.entity.ProductVariantEntity
import com.dakota.pos.data.local.entity.RecipeEntity
import com.dakota.pos.data.local.entity.RecipeItemEntity
import com.dakota.pos.data.local.entity.StockMovementEntity
import com.dakota.pos.data.local.entity.StockOpnameEntity
import com.dakota.pos.data.local.entity.StockOpnameItemEntity
import com.dakota.pos.data.local.dao.CashMovementDao
import com.dakota.pos.data.local.dao.CashierShiftDao
import com.dakota.pos.data.local.dao.GoodsReceiptDao
import com.dakota.pos.data.local.dao.PurchaseOrderDao
import com.dakota.pos.data.local.dao.SupplierDao
import com.dakota.pos.data.local.entity.CashMovementEntity
import com.dakota.pos.data.local.entity.CashierShiftEntity
import com.dakota.pos.data.local.entity.GoodsReceiptEntity
import com.dakota.pos.data.local.entity.GoodsReceiptItemEntity
import com.dakota.pos.data.local.entity.PurchaseOrderEntity
import com.dakota.pos.data.local.entity.PurchaseOrderItemEntity
import com.dakota.pos.data.local.entity.SupplierEntity
import com.dakota.pos.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        AuditLogEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        ProductVariantEntity::class,
        ModifierEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        IngredientEntity::class,
        RecipeEntity::class,
        RecipeItemEntity::class,
        StockMovementEntity::class,
        StockOpnameEntity::class,
        StockOpnameItemEntity::class,
        SupplierEntity::class,
        PurchaseOrderEntity::class,
        PurchaseOrderItemEntity::class,
        GoodsReceiptEntity::class,
        GoodsReceiptItemEntity::class,
        CashierShiftEntity::class,
        CashMovementEntity::class,
        com.dakota.pos.data.local.entity.KitchenOrderEntity::class,
        com.dakota.pos.data.local.entity.KitchenOrderItemEntity::class,
        com.dakota.pos.data.local.entity.StationConfigEntity::class,
        com.dakota.pos.data.local.entity.OperatingExpenseEntity::class,
        com.dakota.pos.data.local.entity.StaffAttendanceEntity::class,
        com.dakota.pos.data.local.entity.StaffPayrollEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DakotaDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun recipeDao(): RecipeDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun stockOpnameDao(): StockOpnameDao
    abstract fun supplierDao(): SupplierDao
    abstract fun purchaseOrderDao(): PurchaseOrderDao
    abstract fun goodsReceiptDao(): GoodsReceiptDao
    abstract fun cashierShiftDao(): CashierShiftDao
    abstract fun cashMovementDao(): CashMovementDao
    abstract fun kitchenOrderDao(): com.dakota.pos.data.local.dao.KitchenOrderDao
    abstract fun stationConfigDao(): com.dakota.pos.data.local.dao.StationConfigDao
    abstract fun operatingExpenseDao(): com.dakota.pos.data.local.dao.OperatingExpenseDao
    abstract fun staffAttendanceDao(): com.dakota.pos.data.local.dao.StaffAttendanceDao
    abstract fun staffPayrollDao(): com.dakota.pos.data.local.dao.StaffPayrollDao

    companion object {
        @Volatile
        private var INSTANCE: DakotaDatabase? = null

        fun getInstance(context: Context): DakotaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DakotaDatabase::class.java,
                    "dakota_pos.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
