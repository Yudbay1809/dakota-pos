package com.dakota.pos.domain.usecase.purchasing

import com.dakota.pos.domain.model.purchasing.GoodsReceipt
import com.dakota.pos.domain.model.purchasing.PurchaseOrder
import com.dakota.pos.domain.model.purchasing.PurchaseOrderStatus
import com.dakota.pos.domain.model.purchasing.Supplier
import com.dakota.pos.domain.model.recipe.Ingredient
import com.dakota.pos.domain.repository.PurchasingRepository
import com.dakota.pos.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow

class GetPurchasingDataUseCase(
    private val purchasingRepository: PurchasingRepository,
    private val recipeRepository: RecipeRepository
) {
    fun getSuppliersFlow(): Flow<List<Supplier>> = purchasingRepository.getAllSuppliersFlow()

    fun getActiveSuppliersFlow(): Flow<List<Supplier>> = purchasingRepository.getActiveSuppliersFlow()

    suspend fun getSupplierById(id: String): Supplier? = purchasingRepository.getSupplierById(id)

    suspend fun saveSupplier(supplier: Supplier) = purchasingRepository.saveSupplier(supplier)

    suspend fun deleteSupplier(id: String) = purchasingRepository.deleteSupplier(id)

    suspend fun seedInitialSuppliers() = purchasingRepository.seedInitialSuppliersIfEmpty()

    fun getPurchaseOrdersFlow(): Flow<List<PurchaseOrder>> = purchasingRepository.getAllPurchaseOrdersFlow()

    fun getPurchaseOrdersByStatusFlow(status: PurchaseOrderStatus): Flow<List<PurchaseOrder>> =
        purchasingRepository.getPurchaseOrdersByStatusFlow(status)

    suspend fun getPurchaseOrderById(id: String): PurchaseOrder? = purchasingRepository.getPurchaseOrderById(id)

    suspend fun updateOrderStatus(id: String, status: PurchaseOrderStatus) =
        purchasingRepository.updateOrderStatus(id, status)

    fun getGoodsReceiptsFlow(): Flow<List<GoodsReceipt>> = purchasingRepository.getAllGoodsReceiptsFlow()

    fun getIngredientsFlow(): Flow<List<Ingredient>> = recipeRepository.getAllIngredientsFlow()

    suspend fun getAllIngredients(): List<Ingredient> = recipeRepository.getAllIngredients()
}
