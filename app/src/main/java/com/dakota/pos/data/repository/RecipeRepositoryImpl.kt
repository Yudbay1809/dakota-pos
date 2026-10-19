package com.dakota.pos.data.repository

import com.dakota.pos.data.local.dao.IngredientDao
import com.dakota.pos.data.local.dao.RecipeDao
import com.dakota.pos.data.local.entity.IngredientEntity
import com.dakota.pos.data.local.entity.RecipeEntity
import com.dakota.pos.data.local.entity.RecipeItemEntity
import com.dakota.pos.domain.model.recipe.HppBreakdown
import com.dakota.pos.domain.model.recipe.Ingredient
import com.dakota.pos.domain.model.recipe.Recipe
import com.dakota.pos.domain.model.recipe.RecipeItem
import com.dakota.pos.domain.model.unit.UnitType
import com.dakota.pos.domain.recipe.HppCalculator
import com.dakota.pos.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class RecipeRepositoryImpl(
    private val ingredientDao: IngredientDao,
    private val recipeDao: RecipeDao
) : RecipeRepository {

    override fun getAllIngredientsFlow(): Flow<List<Ingredient>> {
        return ingredientDao.getAllIngredientsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllIngredients(): List<Ingredient> {
        return ingredientDao.getAllIngredients().map { it.toDomain() }
    }

    override suspend fun getIngredientById(id: String): Ingredient? {
        return ingredientDao.getIngredientById(id)?.toDomain()
    }

    override suspend fun saveIngredient(ingredient: Ingredient) {
        ingredientDao.insertIngredient(ingredient.toEntity())
    }

    override suspend fun deleteIngredient(ingredient: Ingredient) {
        ingredientDao.deleteIngredient(ingredient.toEntity())
    }

    override fun getAllRecipesFlow(): Flow<List<Recipe>> {
        return recipeDao.getAllRecipesFlow().map { entities ->
            entities.map { entity ->
                val items = recipeDao.getItemsForRecipe(entity.id)
                entity.toDomain(items.map { it.toDomain() })
            }
        }
    }

    override suspend fun getRecipeForProduct(productId: String, variantId: String?): Recipe? {
        val entity = recipeDao.getRecipeForProduct(productId, variantId)
            ?: recipeDao.getDefaultRecipeForProduct(productId)
            ?: return null

        val items = recipeDao.getItemsForRecipe(entity.id)
        return entity.toDomain(items.map { it.toDomain() })
    }

    override suspend fun getRecipeById(recipeId: String): Recipe? {
        val entity = recipeDao.getRecipeById(recipeId) ?: return null
        val items = recipeDao.getItemsForRecipe(recipeId)
        return entity.toDomain(items.map { it.toDomain() })
    }

    override suspend fun saveRecipe(recipe: Recipe) {
        val recipeEntity = recipe.toEntity()
        val itemEntities = recipe.items.map { it.toEntity(recipe.id) }
        recipeDao.saveFullRecipe(recipeEntity, itemEntities)
    }

    override suspend fun deleteRecipe(recipeId: String) {
        recipeDao.deleteRecipe(recipeId)
    }

    override suspend fun calculateHppForProduct(
        productId: String,
        variantId: String?,
        sellingPrice: Double
    ): HppBreakdown? {
        val recipe = getRecipeForProduct(productId, variantId) ?: return null
        val allIngredients = getAllIngredients().associateBy { it.id }
        return HppCalculator.calculate(
            recipe = recipe,
            ingredientsMap = allIngredients,
            sellingPrice = sellingPrice
        )
    }

    override suspend fun seedInitialIngredientsAndRecipesIfEmpty() {
        if (ingredientDao.getIngredientCount() > 0) return

        // --- 1. Seed Bahan Baku (Ingredients) ---
        val ingredients = listOf(
            IngredientEntity(
                id = "ing-coffee-beans",
                name = "Biji Kopi House Blend",
                sku = "ING-BEANS-01",
                category = "Bahan Kopi",
                defaultUnit = "kg",
                costPerDefaultUnit = 250000.0, // Rp 250.000 / kg -> Rp 250 / g
                minimumStock = 5.0,
                currentStock = 20.0
            ),
            IngredientEntity(
                id = "ing-fresh-milk",
                name = "Susu Segar (Fresh Milk)",
                sku = "ING-MILK-01",
                category = "Dairy",
                defaultUnit = "L",
                costPerDefaultUnit = 20000.0, // Rp 20.000 / L -> Rp 20 / ml
                minimumStock = 10.0,
                currentStock = 50.0
            ),
            IngredientEntity(
                id = "ing-aren-syrup",
                name = "Gula Aren Cair",
                sku = "ING-SYRUP-AREN",
                category = "Sirup & Pemanis",
                defaultUnit = "L",
                costPerDefaultUnit = 50000.0, // Rp 50.000 / L -> Rp 50 / ml
                minimumStock = 5.0,
                currentStock = 15.0
            ),
            IngredientEntity(
                id = "ing-ice",
                name = "Es Batu Kristal",
                sku = "ING-ICE-01",
                category = "Pelengkap",
                defaultUnit = "g",
                costPerDefaultUnit = 200.0 / 150.0, // Rp 200 per 150g -> Rp 1.33333333 / g
                minimumStock = 10000.0,
                currentStock = 50000.0
            ),
            IngredientEntity(
                id = "ing-cup-cold",
                name = "Cup Plastik 16oz",
                sku = "ING-PACK-CUP16",
                category = "Kemasan",
                defaultUnit = "pcs",
                costPerDefaultUnit = 500.0,
                minimumStock = 100.0,
                currentStock = 1000.0
            ),
            IngredientEntity(
                id = "ing-lid",
                name = "Tutup Lid Cup",
                sku = "ING-PACK-LID",
                category = "Kemasan",
                defaultUnit = "pcs",
                costPerDefaultUnit = 150.0,
                minimumStock = 100.0,
                currentStock = 1000.0
            ),
            IngredientEntity(
                id = "ing-straw",
                name = "Sedotan Higienis",
                sku = "ING-PACK-STRAW",
                category = "Kemasan",
                defaultUnit = "pcs",
                costPerDefaultUnit = 100.0,
                minimumStock = 100.0,
                currentStock = 1000.0
            ),
            IngredientEntity(
                id = "ing-vanilla-syrup",
                name = "Sirup Vanilla Import",
                sku = "ING-SYRUP-VAN",
                category = "Sirup & Pemanis",
                defaultUnit = "L",
                costPerDefaultUnit = 90000.0, // Rp 90 / ml
                minimumStock = 2.0,
                currentStock = 8.0
            ),
            IngredientEntity(
                id = "ing-matcha-powder",
                name = "Matcha Uji Premium",
                sku = "ING-MATCHA-01",
                category = "Bubuk Minuman",
                defaultUnit = "kg",
                costPerDefaultUnit = 300000.0, // Rp 300 / g
                minimumStock = 2.0,
                currentStock = 5.0
            ),
            IngredientEntity(
                id = "ing-choco-powder",
                name = "Dark Chocolate Powder",
                sku = "ING-CHOCO-01",
                category = "Bubuk Minuman",
                defaultUnit = "kg",
                costPerDefaultUnit = 150000.0, // Rp 150 / g
                minimumStock = 3.0,
                currentStock = 10.0
            )
        )
        ingredientDao.insertIngredients(ingredients)

        // --- 2. Seed Resep Awal (Recipes) ---
        // Resep 1: Es Kopi Susu Dakota (Kasus Spesifikasi Fase 7 -> HPP Rp 8.200)
        val recipeKopiSusuId = "rcp-kopi-susu"
        val recipeKopiSusu = RecipeEntity(
            id = recipeKopiSusuId,
            productId = "prd-latte",
            productName = "Caffé Latte",
            variantId = "prd-latte-iced-reg",
            variantName = "Iced / Regular",
            notes = "Standard Es Kopi Susu Dakota Signature"
        )
        val itemsKopiSusu = listOf(
            RecipeItemEntity(
                id = UUID.randomUUID().toString(),
                recipeId = recipeKopiSusuId,
                ingredientId = "ing-coffee-beans",
                ingredientName = "Biji Kopi House Blend",
                quantity = 18.0,
                unit = "g"
            ),
            RecipeItemEntity(
                id = UUID.randomUUID().toString(),
                recipeId = recipeKopiSusuId,
                ingredientId = "ing-fresh-milk",
                ingredientName = "Susu Segar (Fresh Milk)",
                quantity = 100.0,
                unit = "ml"
            ),
            RecipeItemEntity(
                id = UUID.randomUUID().toString(),
                recipeId = recipeKopiSusuId,
                ingredientId = "ing-aren-syrup",
                ingredientName = "Gula Aren Cair",
                quantity = 15.0,
                unit = "ml"
            ),
            RecipeItemEntity(
                id = UUID.randomUUID().toString(),
                recipeId = recipeKopiSusuId,
                ingredientId = "ing-ice",
                ingredientName = "Es Batu Kristal",
                quantity = 150.0,
                unit = "g"
            ),
            RecipeItemEntity(
                id = UUID.randomUUID().toString(),
                recipeId = recipeKopiSusuId,
                ingredientId = "ing-cup-cold",
                ingredientName = "Cup Plastik 16oz",
                quantity = 1.0,
                unit = "pcs"
            ),
            RecipeItemEntity(
                id = UUID.randomUUID().toString(),
                recipeId = recipeKopiSusuId,
                ingredientId = "ing-lid",
                ingredientName = "Tutup Lid Cup",
                quantity = 1.0,
                unit = "pcs"
            ),
            RecipeItemEntity(
                id = UUID.randomUUID().toString(),
                recipeId = recipeKopiSusuId,
                ingredientId = "ing-straw",
                ingredientName = "Sedotan Higienis",
                quantity = 1.0,
                unit = "pcs"
            )
        )
        recipeDao.saveFullRecipe(recipeKopiSusu, itemsKopiSusu)

        // Resep 2: Americano (Hot / Regular)
        val recipeAmericanoId = "rcp-americano"
        val recipeAmericano = RecipeEntity(
            id = recipeAmericanoId,
            productId = "prd-americano",
            productName = "Americano",
            variantId = "prd-americano-hot-reg",
            variantName = "Hot / Regular",
            notes = "Single Double Espresso Americano"
        )
        val itemsAmericano = listOf(
            RecipeItemEntity(
                id = UUID.randomUUID().toString(),
                recipeId = recipeAmericanoId,
                ingredientId = "ing-coffee-beans",
                ingredientName = "Biji Kopi House Blend",
                quantity = 18.0,
                unit = "g"
            ),
            RecipeItemEntity(
                id = UUID.randomUUID().toString(),
                recipeId = recipeAmericanoId,
                ingredientId = "ing-cup-cold",
                ingredientName = "Cup Plastik 16oz",
                quantity = 1.0,
                unit = "pcs"
            ),
            RecipeItemEntity(
                id = UUID.randomUUID().toString(),
                recipeId = recipeAmericanoId,
                ingredientId = "ing-lid",
                ingredientName = "Tutup Lid Cup",
                quantity = 1.0,
                unit = "pcs"
            )
        )
        recipeDao.saveFullRecipe(recipeAmericano, itemsAmericano)
    }

    // --- Mappers ---

    private fun IngredientEntity.toDomain() = Ingredient(
        id = id,
        name = name,
        sku = sku,
        category = category,
        defaultUnit = UnitType.fromSymbol(defaultUnit),
        costPerDefaultUnit = costPerDefaultUnit,
        minimumStock = minimumStock,
        currentStock = currentStock,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Ingredient.toEntity() = IngredientEntity(
        id = id,
        name = name,
        sku = sku,
        category = category,
        defaultUnit = defaultUnit.symbol,
        costPerDefaultUnit = costPerDefaultUnit,
        minimumStock = minimumStock,
        currentStock = currentStock,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun RecipeEntity.toDomain(items: List<RecipeItem>) = Recipe(
        id = id,
        productId = productId,
        productName = productName,
        variantId = variantId,
        variantName = variantName,
        items = items,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Recipe.toEntity() = RecipeEntity(
        id = id,
        productId = productId,
        productName = productName,
        variantId = variantId,
        variantName = variantName,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun RecipeItemEntity.toDomain() = RecipeItem(
        id = id,
        recipeId = recipeId,
        ingredientId = ingredientId,
        ingredientName = ingredientName,
        quantity = quantity,
        unit = UnitType.fromSymbol(unit),
        notes = notes
    )

    private fun RecipeItem.toEntity(recipeId: String) = RecipeItemEntity(
        id = id.ifBlank { UUID.randomUUID().toString() },
        recipeId = recipeId,
        ingredientId = ingredientId,
        ingredientName = ingredientName,
        quantity = quantity,
        unit = unit.symbol,
        notes = notes
    )
}
