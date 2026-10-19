package com.dakota.pos.domain.repository

import com.dakota.pos.domain.model.Category
import com.dakota.pos.domain.model.Modifier
import com.dakota.pos.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getCategories(): Flow<List<Category>>
    fun getProducts(): Flow<List<Product>>
    fun getProductsByCategory(categoryId: String): Flow<List<Product>>
    suspend fun getProductById(productId: String): Product?
    fun getModifiers(): Flow<List<Modifier>>
    suspend fun seedInitialCatalogIfEmpty()
}
