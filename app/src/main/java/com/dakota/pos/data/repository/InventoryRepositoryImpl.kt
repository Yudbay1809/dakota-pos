package com.dakota.pos.data.repository

import com.dakota.pos.data.local.dao.IngredientDao
import com.dakota.pos.data.local.dao.StockMovementDao
import com.dakota.pos.data.local.dao.StockOpnameDao
import com.dakota.pos.data.local.entity.StockMovementEntity
import com.dakota.pos.data.local.entity.StockOpnameEntity
import com.dakota.pos.data.local.entity.StockOpnameItemEntity
import com.dakota.pos.domain.model.inventory.MovementType
import com.dakota.pos.domain.model.inventory.StockMovement
import com.dakota.pos.domain.model.inventory.StockOpname
import com.dakota.pos.domain.model.inventory.StockOpnameItem
import com.dakota.pos.domain.model.recipe.Ingredient
import com.dakota.pos.domain.model.unit.UnitType
import com.dakota.pos.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InventoryRepositoryImpl(
    private val ingredientDao: IngredientDao,
    private val stockMovementDao: StockMovementDao,
    private val stockOpnameDao: StockOpnameDao
) : InventoryRepository {

    // ─── Stock Movements ────────────────────────────────────────────────

    override suspend fun recordMovement(movement: StockMovement) {
        stockMovementDao.insertMovement(movement.toEntity())
    }

    override suspend fun recordMovements(movements: List<StockMovement>) {
        stockMovementDao.insertMovements(movements.map { it.toEntity() })
    }

    override fun getMovementsForIngredient(ingredientId: String): Flow<List<StockMovement>> {
        return stockMovementDao.getMovementsForIngredient(ingredientId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentMovements(limit: Int): Flow<List<StockMovement>> {
        return stockMovementDao.getRecentMovements(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMovementsByType(type: MovementType): Flow<List<StockMovement>> {
        return stockMovementDao.getMovementsByType(type).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getMovementsByReferenceId(referenceId: String): List<StockMovement> {
        return stockMovementDao.getMovementsByReferenceId(referenceId).map { it.toDomain() }
    }

    override suspend fun hasBeenDeductedForOrder(orderId: String): Boolean {
        return stockMovementDao.countSaleMovementsForOrder(orderId) > 0
    }

    // ─── Stock Opname ───────────────────────────────────────────────────

    override suspend fun saveStockOpname(opname: StockOpname) {
        val entity = StockOpnameEntity(
            id = opname.id,
            opnameNumber = opname.opnameNumber,
            performedByUserId = opname.performedByUserId,
            performedByUserName = opname.performedByUserName,
            notes = opname.notes,
            timestamp = opname.timestamp
        )
        val items = opname.items.map { item ->
            StockOpnameItemEntity(
                id = item.id,
                opnameId = item.opnameId,
                ingredientId = item.ingredientId,
                ingredientName = item.ingredientName,
                systemStock = item.systemStock,
                physicalStock = item.physicalStock,
                difference = item.difference,
                unit = item.unit.symbol,
                notes = item.notes
            )
        }
        stockOpnameDao.saveFullOpname(entity, items)
    }

    override fun getAllOpnamesFlow(): Flow<List<StockOpname>> {
        return stockOpnameDao.getAllOpnamesFlow().map { entities ->
            entities.map { entity ->
                val items = stockOpnameDao.getItemsForOpname(entity.id).map { it.toDomain() }
                StockOpname(
                    id = entity.id,
                    opnameNumber = entity.opnameNumber,
                    performedByUserId = entity.performedByUserId,
                    performedByUserName = entity.performedByUserName,
                    items = items,
                    notes = entity.notes,
                    timestamp = entity.timestamp
                )
            }
        }
    }

    // ─── Ingredient Stock Updates ───────────────────────────────────────

    override suspend fun updateIngredientStock(ingredientId: String, newStock: Double) {
        ingredientDao.updateCurrentStock(ingredientId, newStock)
    }

    override suspend fun getIngredientById(ingredientId: String): Ingredient? {
        return ingredientDao.getIngredientById(ingredientId)?.let { entity ->
            Ingredient(
                id = entity.id,
                name = entity.name,
                sku = entity.sku,
                category = entity.category,
                defaultUnit = UnitType.fromSymbol(entity.defaultUnit),
                costPerDefaultUnit = entity.costPerDefaultUnit,
                minimumStock = entity.minimumStock,
                currentStock = entity.currentStock,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }

    override suspend fun getAllIngredients(): List<Ingredient> {
        return ingredientDao.getAllIngredients().map { entity ->
            Ingredient(
                id = entity.id,
                name = entity.name,
                sku = entity.sku,
                category = entity.category,
                defaultUnit = UnitType.fromSymbol(entity.defaultUnit),
                costPerDefaultUnit = entity.costPerDefaultUnit,
                minimumStock = entity.minimumStock,
                currentStock = entity.currentStock,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }

    override fun getAllIngredientsFlow(): Flow<List<Ingredient>> {
        return ingredientDao.getAllIngredientsFlow().map { entities ->
            entities.map { entity ->
                Ingredient(
                    id = entity.id,
                    name = entity.name,
                    sku = entity.sku,
                    category = entity.category,
                    defaultUnit = UnitType.fromSymbol(entity.defaultUnit),
                    costPerDefaultUnit = entity.costPerDefaultUnit,
                    minimumStock = entity.minimumStock,
                    currentStock = entity.currentStock,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt
                )
            }
        }
    }

    // ─── Mappers ────────────────────────────────────────────────────────

    private fun StockMovement.toEntity() = StockMovementEntity(
        id = id,
        ingredientId = ingredientId,
        ingredientName = ingredientName,
        type = type,
        quantityChange = quantityChange,
        unit = unit.symbol,
        balanceBefore = balanceBefore,
        balanceAfter = balanceAfter,
        referenceId = referenceId,
        notes = notes,
        userId = userId,
        userName = userName,
        timestamp = timestamp
    )

    private fun StockMovementEntity.toDomain() = StockMovement(
        id = id,
        ingredientId = ingredientId,
        ingredientName = ingredientName,
        type = type,
        quantityChange = quantityChange,
        unit = UnitType.fromSymbol(unit),
        balanceBefore = balanceBefore,
        balanceAfter = balanceAfter,
        referenceId = referenceId,
        notes = notes,
        userId = userId,
        userName = userName,
        timestamp = timestamp
    )

    private fun StockOpnameItemEntity.toDomain() = StockOpnameItem(
        id = id,
        opnameId = opnameId,
        ingredientId = ingredientId,
        ingredientName = ingredientName,
        systemStock = systemStock,
        physicalStock = physicalStock,
        difference = difference,
        unit = UnitType.fromSymbol(unit),
        notes = notes
    )
}
