package com.dakota.pos.presentation.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dakota.pos.domain.model.Product
import com.dakota.pos.domain.model.ProductVariant
import com.dakota.pos.domain.model.recipe.HppBreakdown
import com.dakota.pos.domain.model.recipe.Ingredient
import com.dakota.pos.domain.model.recipe.Recipe
import com.dakota.pos.domain.model.recipe.RecipeItem
import com.dakota.pos.domain.model.unit.UnitType
import com.dakota.pos.domain.repository.ProductRepository
import com.dakota.pos.domain.repository.RecipeRepository
import com.dakota.pos.domain.usecase.recipe.CalculateHppUseCase
import com.dakota.pos.domain.usecase.recipe.GetIngredientsUseCase
import com.dakota.pos.domain.usecase.recipe.SaveIngredientUseCase
import com.dakota.pos.domain.usecase.recipe.SaveRecipeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class RecipeUiState(
    val products: List<Product> = emptyList(),
    val ingredients: List<Ingredient> = emptyList(),
    val selectedProduct: Product? = null,
    val selectedVariant: ProductVariant? = null,
    val currentRecipe: Recipe? = null,
    val hppBreakdown: HppBreakdown? = null,
    val activeTab: RecipeTab = RecipeTab.HPP_CALCULATOR,
    val isLoading: Boolean = true,
    val statusMessage: String? = null,
    val errorMessage: String? = null,

    // Ingredient edit dialog
    val showIngredientDialog: Boolean = false,
    val editingIngredient: Ingredient? = null
)

enum class RecipeTab {
    HPP_CALCULATOR,
    INGREDIENTS_LIST
}

class RecipeViewModel(
    private val recipeRepository: RecipeRepository,
    private val productRepository: ProductRepository,
    private val calculateHppUseCase: CalculateHppUseCase,
    private val getIngredientsUseCase: GetIngredientsUseCase,
    private val saveIngredientUseCase: SaveIngredientUseCase,
    private val saveRecipeUseCase: SaveRecipeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeUiState())
    val uiState: StateFlow<RecipeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            recipeRepository.seedInitialIngredientsAndRecipesIfEmpty()

            val products: List<Product> = productRepository.getProducts().firstOrNull() ?: emptyList()
            val ingredients = recipeRepository.getAllIngredients()

            val defaultProductEntity = products.firstOrNull()
            val defaultFullProduct: Product? = if (defaultProductEntity != null) {
                productRepository.getProductById(defaultProductEntity.id)
            } else {
                null
            }
            val defaultVariant = defaultFullProduct?.variants?.firstOrNull { it.isDefault }
                ?: defaultFullProduct?.variants?.firstOrNull()

            _uiState.update {
                it.copy(
                    products = products,
                    ingredients = ingredients,
                    selectedProduct = defaultFullProduct,
                    selectedVariant = defaultVariant,
                    isLoading = false
                )
            }

            if (defaultFullProduct != null) {
                loadRecipeAndHpp(defaultFullProduct, defaultVariant)
            }
        }
    }

    fun selectTab(tab: RecipeTab) {
        _uiState.update { it.copy(activeTab = tab, statusMessage = null, errorMessage = null) }
    }

    fun selectProduct(product: Product) {
        viewModelScope.launch {
            val fullProduct = productRepository.getProductById(product.id) ?: product
            val defaultVariant = fullProduct.variants.firstOrNull { it.isDefault }
                ?: fullProduct.variants.firstOrNull()

            _uiState.update {
                it.copy(
                    selectedProduct = fullProduct,
                    selectedVariant = defaultVariant,
                    statusMessage = null,
                    errorMessage = null
                )
            }
            loadRecipeAndHpp(fullProduct, defaultVariant)
        }
    }

    fun selectVariant(variant: ProductVariant) {
        val currentProduct = _uiState.value.selectedProduct ?: return
        _uiState.update { it.copy(selectedVariant = variant) }
        loadRecipeAndHpp(currentProduct, variant)
    }

    private fun loadRecipeAndHpp(product: Product, variant: ProductVariant?) {
        viewModelScope.launch {
            val recipe = recipeRepository.getRecipeForProduct(product.id, variant?.id)
            val sellingPrice = product.basePrice + (variant?.priceAdjustment ?: 0.0)

            val breakdown = calculateHppUseCase(
                productId = product.id,
                variantId = variant?.id,
                sellingPrice = sellingPrice
            )

            _uiState.update {
                it.copy(
                    currentRecipe = recipe,
                    hppBreakdown = breakdown
                )
            }
        }
    }

    // --- Ingredient Management ---

    fun openAddIngredientDialog() {
        _uiState.update {
            it.copy(
                showIngredientDialog = true,
                editingIngredient = null
            )
        }
    }

    fun openEditIngredientDialog(ingredient: Ingredient) {
        _uiState.update {
            it.copy(
                showIngredientDialog = true,
                editingIngredient = ingredient
            )
        }
    }

    fun dismissIngredientDialog() {
        _uiState.update { it.copy(showIngredientDialog = false, editingIngredient = null) }
    }

    fun saveIngredient(
        name: String,
        sku: String,
        category: String,
        unit: UnitType,
        cost: Double,
        minimumStock: Double
    ) {
        viewModelScope.launch {
            val current = _uiState.value.editingIngredient
            val ingredient = Ingredient(
                id = current?.id ?: UUID.randomUUID().toString(),
                name = name.trim(),
                sku = sku.trim().ifBlank { "ING-${System.currentTimeMillis()}" },
                category = category.trim().ifBlank { "Bahan Baku" },
                defaultUnit = unit,
                costPerDefaultUnit = cost,
                minimumStock = minimumStock,
                currentStock = current?.currentStock ?: 0.0
            )

            val result = saveIngredientUseCase(ingredient)
            if (result.isSuccess) {
                val updatedIngredients = recipeRepository.getAllIngredients()
                _uiState.update {
                    it.copy(
                        ingredients = updatedIngredients,
                        showIngredientDialog = false,
                        editingIngredient = null,
                        statusMessage = "Bahan baku '${ingredient.name}' berhasil disimpan!"
                    )
                }
                // Refresh current HPP
                val p = _uiState.value.selectedProduct
                val v = _uiState.value.selectedVariant
                if (p != null) loadRecipeAndHpp(p, v)
            } else {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Gagal menyimpan bahan baku")
                }
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
    }
}
