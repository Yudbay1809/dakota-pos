package com.dakota.pos.data.repository

import com.dakota.pos.data.local.dao.CategoryDao
import com.dakota.pos.data.local.dao.ProductDao
import com.dakota.pos.data.local.entity.CategoryEntity
import com.dakota.pos.data.local.entity.ModifierEntity
import com.dakota.pos.data.local.entity.ProductEntity
import com.dakota.pos.data.local.entity.ProductVariantEntity
import com.dakota.pos.domain.model.Category
import com.dakota.pos.domain.model.Modifier
import com.dakota.pos.domain.model.Product
import com.dakota.pos.domain.model.ProductVariant
import com.dakota.pos.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class ProductRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao
) : ProductRepository {

    override fun getCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getProducts(): Flow<List<Product>> {
        return combine(
            productDao.getAllActiveProducts(),
            productDao.getAllActiveModifiers() // We need variants per product
        ) { products, _ ->
            products.map { entity ->
                // For the Flow-based listing, we don't eagerly load variants
                // Variants are loaded on-demand when a product is selected
                entity.toDomain(emptyList())
            }
        }
    }

    override fun getProductsByCategory(categoryId: String): Flow<List<Product>> {
        return productDao.getProductsByCategory(categoryId).map { entities ->
            entities.map { entity ->
                entity.toDomain(emptyList())
            }
        }
    }

    override suspend fun getProductById(productId: String): Product? {
        val entity = productDao.getProductById(productId) ?: return null
        val variants = productDao.getVariantsByProductIdSync(productId)
        return entity.toDomain(variants.map { it.toDomain() })
    }

    override fun getModifiers(): Flow<List<Modifier>> {
        return productDao.getAllActiveModifiers().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun seedInitialCatalogIfEmpty() {
        if (categoryDao.getCategoryCount() > 0) return

        // --- Categories ---
        val categories = listOf(
            CategoryEntity(id = "cat-coffee", name = "Kopi", displayOrder = 0, iconName = "coffee"),
            CategoryEntity(id = "cat-noncoffee", name = "Non-Kopi", displayOrder = 1, iconName = "local_cafe"),
            CategoryEntity(id = "cat-food", name = "Makanan", displayOrder = 2, iconName = "restaurant"),
            CategoryEntity(id = "cat-dessert", name = "Dessert", displayOrder = 3, iconName = "cake"),
            CategoryEntity(id = "cat-snack", name = "Snack", displayOrder = 4, iconName = "cookie")
        )
        categoryDao.insertCategories(categories)

        // --- Products ---
        val products = listOf(
            // Coffee
            ProductEntity(id = "prd-americano", categoryId = "cat-coffee", name = "Americano", description = "Espresso + Air Panas", basePrice = 18000.0, colorHex = "#4E342E"),
            ProductEntity(id = "prd-latte", categoryId = "cat-coffee", name = "Caffé Latte", description = "Espresso + Steamed Milk", basePrice = 25000.0, colorHex = "#6D4C41"),
            ProductEntity(id = "prd-cappuccino", categoryId = "cat-coffee", name = "Cappuccino", description = "Espresso + Foam Milk", basePrice = 25000.0, colorHex = "#795548"),
            ProductEntity(id = "prd-vl", categoryId = "cat-coffee", name = "Vanilla Latte", description = "Espresso + Vanilla Syrup + Milk", basePrice = 28000.0, colorHex = "#8D6E63"),
            ProductEntity(id = "prd-mocha", categoryId = "cat-coffee", name = "Caffé Mocha", description = "Espresso + Chocolate + Milk", basePrice = 28000.0, colorHex = "#5D4037"),
            ProductEntity(id = "prd-espresso", categoryId = "cat-coffee", name = "Espresso", description = "Double Shot Espresso", basePrice = 15000.0, colorHex = "#3E2723"),

            // Non-Coffee
            ProductEntity(id = "prd-matcha", categoryId = "cat-noncoffee", name = "Matcha Latte", description = "Matcha Premium + Milk", basePrice = 25000.0, colorHex = "#558B2F"),
            ProductEntity(id = "prd-choco", categoryId = "cat-noncoffee", name = "Cokelat Panas", description = "Premium Chocolate + Milk", basePrice = 22000.0, colorHex = "#4E342E"),
            ProductEntity(id = "prd-thai-tea", categoryId = "cat-noncoffee", name = "Thai Tea", description = "Thai Tea Classic + Susu", basePrice = 20000.0, colorHex = "#E65100"),
            ProductEntity(id = "prd-lemon-tea", categoryId = "cat-noncoffee", name = "Lemon Tea", description = "Teh + Perasan Lemon Segar", basePrice = 15000.0, colorHex = "#F9A825"),

            // Food
            ProductEntity(id = "prd-croissant", categoryId = "cat-food", name = "Croissant", description = "Butter Croissant Premium", basePrice = 20000.0, colorHex = "#FF8F00"),
            ProductEntity(id = "prd-sandwich", categoryId = "cat-food", name = "Club Sandwich", description = "Roti + Ayam + Sayuran", basePrice = 30000.0, colorHex = "#EF6C00"),
            ProductEntity(id = "prd-roti-bakar", categoryId = "cat-food", name = "Roti Bakar", description = "Roti Bakar Selai Cokelat/Keju", basePrice = 18000.0, colorHex = "#F57F17"),

            // Dessert
            ProductEntity(id = "prd-cheesecake", categoryId = "cat-dessert", name = "Cheesecake", description = "New York Cheesecake", basePrice = 35000.0, colorHex = "#FDD835"),
            ProductEntity(id = "prd-brownies", categoryId = "cat-dessert", name = "Brownies", description = "Double Chocolate Brownies", basePrice = 22000.0, colorHex = "#4E342E"),

            // Snack
            ProductEntity(id = "prd-fries", categoryId = "cat-snack", name = "French Fries", description = "Kentang Goreng Renyah", basePrice = 18000.0, colorHex = "#FFB300"),
            ProductEntity(id = "prd-nachos", categoryId = "cat-snack", name = "Nachos", description = "Nachos + Cheese Sauce", basePrice = 25000.0, colorHex = "#FF6F00")
        )
        productDao.insertProducts(products)

        // --- Product Variants (Size-based: Hot/Iced x Regular/Large) ---
        val variants = mutableListOf<ProductVariantEntity>()
        // Coffee variants
        val coffeeProductIds = listOf("prd-americano", "prd-latte", "prd-cappuccino", "prd-vl", "prd-mocha")
        coffeeProductIds.forEach { pid ->
            variants.addAll(listOf(
                ProductVariantEntity(id = "${pid}-hot-reg", productId = pid, name = "Hot / Regular", priceAdjustment = 0.0, isDefault = true),
                ProductVariantEntity(id = "${pid}-hot-lrg", productId = pid, name = "Hot / Large", priceAdjustment = 5000.0),
                ProductVariantEntity(id = "${pid}-iced-reg", productId = pid, name = "Iced / Regular", priceAdjustment = 3000.0),
                ProductVariantEntity(id = "${pid}-iced-lrg", productId = pid, name = "Iced / Large", priceAdjustment = 8000.0)
            ))
        }
        // Espresso single variant
        variants.add(ProductVariantEntity(id = "prd-espresso-single", productId = "prd-espresso", name = "Single Shot", priceAdjustment = 0.0, isDefault = true))
        variants.add(ProductVariantEntity(id = "prd-espresso-double", productId = "prd-espresso", name = "Double Shot", priceAdjustment = 5000.0))

        // Non-coffee variants (Hot/Iced)
        listOf("prd-matcha", "prd-choco", "prd-thai-tea").forEach { pid ->
            variants.addAll(listOf(
                ProductVariantEntity(id = "${pid}-hot", productId = pid, name = "Hot", priceAdjustment = 0.0, isDefault = true),
                ProductVariantEntity(id = "${pid}-iced", productId = pid, name = "Iced", priceAdjustment = 3000.0)
            ))
        }
        variants.addAll(listOf(
            ProductVariantEntity(id = "prd-lemon-tea-hot", productId = "prd-lemon-tea", name = "Hot", priceAdjustment = 0.0, isDefault = true),
            ProductVariantEntity(id = "prd-lemon-tea-iced", productId = "prd-lemon-tea", name = "Iced", priceAdjustment = 2000.0)
        ))
        productDao.insertVariants(variants)

        // --- Modifiers ---
        val modifiers = listOf(
            ModifierEntity(id = "mod-extra-shot", name = "Extra Shot Espresso", groupName = "Add-On", price = 5000.0),
            ModifierEntity(id = "mod-oat-milk", name = "Oat Milk", groupName = "Milk Alternative", price = 8000.0),
            ModifierEntity(id = "mod-almond-milk", name = "Almond Milk", groupName = "Milk Alternative", price = 8000.0),
            ModifierEntity(id = "mod-soy-milk", name = "Soy Milk", groupName = "Milk Alternative", price = 5000.0),
            ModifierEntity(id = "mod-less-sugar", name = "Less Sugar", groupName = "Sweetness", price = 0.0),
            ModifierEntity(id = "mod-extra-sugar", name = "Extra Sugar", groupName = "Sweetness", price = 0.0),
            ModifierEntity(id = "mod-whipped-cream", name = "Whipped Cream", groupName = "Topping", price = 5000.0),
            ModifierEntity(id = "mod-caramel-drizzle", name = "Caramel Drizzle", groupName = "Topping", price = 3000.0),
            ModifierEntity(id = "mod-choco-drizzle", name = "Chocolate Drizzle", groupName = "Topping", price = 3000.0)
        )
        productDao.insertModifiers(modifiers)
    }

    // --- Entity → Domain Mappers ---

    private fun CategoryEntity.toDomain() = Category(
        id = id,
        name = name,
        displayOrder = displayOrder,
        iconName = iconName
    )

    private fun ProductEntity.toDomain(variants: List<ProductVariant>) = Product(
        id = id,
        categoryId = categoryId,
        name = name,
        description = description,
        basePrice = basePrice,
        colorHex = colorHex,
        isActive = isActive,
        variants = variants
    )

    private fun ProductVariantEntity.toDomain() = ProductVariant(
        id = id,
        productId = productId,
        name = name,
        priceAdjustment = priceAdjustment,
        isDefault = isDefault
    )

    private fun ModifierEntity.toDomain() = Modifier(
        id = id,
        name = name,
        groupName = groupName,
        price = price,
        isActive = isActive
    )
}
