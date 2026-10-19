package com.dakota.pos.domain.usecase.pos

import com.dakota.pos.domain.model.Category
import com.dakota.pos.domain.model.Modifier
import com.dakota.pos.domain.model.Product
import com.dakota.pos.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class CatalogData(
    val categories: List<Category>,
    val products: List<Product>,
    val modifiers: List<Modifier>
)

class GetCatalogUseCase(
    private val productRepository: ProductRepository
) {
    operator fun invoke(): Flow<CatalogData> {
        return combine(
            productRepository.getCategories(),
            productRepository.getProducts(),
            productRepository.getModifiers()
        ) { categories, products, modifiers ->
            CatalogData(
                categories = categories,
                products = products,
                modifiers = modifiers
            )
        }
    }
}
