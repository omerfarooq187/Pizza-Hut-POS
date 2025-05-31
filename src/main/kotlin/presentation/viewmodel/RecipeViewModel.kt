package presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.model.ItemVariant
import data.model.MenuItemWithVariants
import data.model.RawItem
import data.model.Recipe
import data.repository.MenuRepository
import data.repository.RawItemRepository
import data.repository.RecipeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// presentation/viewmodel/RecipeViewModel.kt
class RecipeViewModel : KoinComponent {
    private val menuRepo: MenuRepository by inject()
    private val rawItemRepo: RawItemRepository by inject()
    private val recipeRepo: RecipeRepository by inject()

    var menuItemsWithVariants by mutableStateOf<List<MenuItemWithVariants>>(emptyList())

    private val _rawItems = mutableStateListOf<RawItem>()
    val rawItems: List<RawItem> get() = _rawItems

    private val _loading = mutableStateOf(true)
    val loading: Boolean get() = _loading.value

    private val _errors = mutableStateListOf<String>()
    val errors: List<String> get() = _errors

    var selectedVariant by mutableStateOf<ItemVariant?>(null)
    var selectedRawItem by mutableStateOf<RawItem?>(null)
    var quantity by mutableStateOf("")

    init {
        loadData()
    }


    fun addRecipe() {
        if (selectedVariant == null || selectedRawItem == null || quantity.isBlank()) {
            _errors.add("Please fill all fields")
            return
        }

        CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                recipeRepo.addRecipe(
                    Recipe(
                        variantId = selectedVariant!!.id,
                        rawItemId = selectedRawItem!!.id,
                        quantityNeeded = quantity.toDouble()
                    )
                )
                loadData()
                clearForm()
            } catch (e: Exception) {
                _errors.add("Failed to add recipe: ${e.message}")
            }
        }
    }

    fun deleteRecipe(variantId: Int, rawItemId: Int) {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                recipeRepo.deleteRecipe(variantId, rawItemId)
                loadData()
            } catch (e: Exception) {
                _errors.add("Failed to delete recipe: ${e.message}")
            }
        }
    }

    suspend fun loadRecipesForVariants() {
        menuItemsWithVariants = menuItemsWithVariants.map { menuItem ->
            val variantsWithRecipes = menuItem.variants.map { variant ->
                val recipes = recipeRepo.getRecipesForVariant(variant.id)
                variant.copy(recipes = recipes)
            }
            menuItem.copy(variants = variantsWithRecipes)
        }
    }

    fun loadData() {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                val menuItems = menuRepo.getAllMenuItemsWithVariants()
                menuItemsWithVariants = menuItems.map { menuItem ->
                    val variantsWithRecipes = menuItem.variants.map { variant ->
                        val recipes = recipeRepo.getRecipesForVariant(variant.id)
                        variant.copy(recipes = recipes)
                    }
                    menuItem.copy(variants = variantsWithRecipes)
                }
                _rawItems.clear()
                _rawItems.addAll(rawItemRepo.getAllRawItems())
            } catch (e: Exception) {
                _errors.add("Failed to load data: ${e.message}")
            } finally {
                _loading.value = false
            }
        }
    }

    fun updateRecipe(oldRecipe: Recipe) {
        if (selectedVariant == null || selectedRawItem == null || quantity.isBlank()) {
            _errors.add("Please fill all fields")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Delete old recipe and add new one
                recipeRepo.deleteRecipe(oldRecipe.variantId, oldRecipe.rawItemId)
                recipeRepo.addRecipe(
                    Recipe(
                        variantId = selectedVariant!!.id,
                        rawItemId = selectedRawItem!!.id,
                        quantityNeeded = quantity.toDouble()
                    )
                )
                clearForm()
            } catch (e: Exception) {
                _errors.add("Failed to update recipe: ${e.message}")
            }
        }
    }
    fun validateForm(): Boolean {
        _errors.clear()

        if (selectedVariant == null) {
            _errors.add("Please select a menu item variant")
        }
        if (selectedRawItem == null) {
            _errors.add("Please select a raw material")
        }
        if (quantity.isBlank() || quantity.toDoubleOrNull() == null) {
            _errors.add("Please enter a valid quantity")
        }

        return _errors.isEmpty()
    }

    fun clearForm() {
        quantity = ""
        _errors.clear()
    }

}