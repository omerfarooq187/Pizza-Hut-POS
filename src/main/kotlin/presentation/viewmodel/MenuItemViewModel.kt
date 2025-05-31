package presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.model.Category
import data.model.DiscountType
import data.model.ItemVariant
import data.model.MenuItem
import data.repository.CategoryRepository
import data.repository.MenuRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MenuItemViewModel: KoinComponent {
    private val coroutineScope = CoroutineScope(Dispatchers.Main.immediate)

    private val menuRepo: MenuRepository by inject()
    private val categoryRepo: CategoryRepository by inject()

    var categories by mutableStateOf(emptyList<Category>())
    val categoryItems = mutableStateMapOf<Int, List<MenuItem>>()
    var currentCategoryId by mutableStateOf<Int?>(null)

    var itemName by mutableStateOf("")
    var description by mutableStateOf("")
    var variants by mutableStateOf<List<ItemVariant>>(emptyList())
    var error by mutableStateOf<String?>(null)
    var loading by mutableStateOf(true)
    var discountValue by mutableStateOf(0.0)
    var discountType by mutableStateOf(DiscountType.FIXED)

    var showCreateItemDialog by mutableStateOf(false)
    private var editingItem by mutableStateOf<MenuItem?>(null)
    var showEditDialog by mutableStateOf(false)

    init {
        loadCategories()
    }

    private fun loadCategories() {
        coroutineScope.launch {
            try {
                categories = (categoryRepo.getAllCategories())
                loading = false
            } catch (e: Exception) {
                error = "Failed to load categories "+e.message
                loading = false
            }
        }
    }



    // Change from single error to map of errors
    val errors = mutableStateMapOf<Int, String?>()

    fun loadItems(categoryId: Int) {
        currentCategoryId = categoryId
        coroutineScope.launch {
            try {
                val items = menuRepo.getItemsByCategory(categoryId)
                categoryItems[categoryId] = items
                errors.remove(categoryId)
            } catch (e: Exception) {
                errors[categoryId] = "Failed to load items: ${e.message}"
            }
        }
    }

    fun saveItem() {
        coroutineScope.launch {
            try {
                if (currentCategoryId == null) {
                    error = "Category context missing"
                    return@launch
                }
                if (itemName.isBlank()) {
                    error = "Item name is required"
                    return@launch
                }
                if (variants.isEmpty()) {
                    error = "At least one variant is required"
                    return@launch
                }
                // Use currentCategoryId directly
                if (menuRepo.itemExists(itemName, currentCategoryId!!)) {
                    error = "Item already exists in the category"
                    return@launch
                }

                menuRepo.createItem(
                    MenuItem(
                        categoryId = currentCategoryId!!,
                        name = itemName,
                        description = description,
                        variants = variants,
                        discountType = discountType,
                        discountValue = discountValue
                    )
                )
                error = null
                clearForm()
                showCreateItemDialog = false
                showEditDialog = false
                loadItems(currentCategoryId!!)
            } catch (e: Exception) {
                e.printStackTrace()  // <--- Logs the full stacktrace in Logcat
                error = "Failed to save item: ${e.message ?: "Unknown error"}"
            }
        }
    }

    fun clearForm() {
        itemName = ""
        description = ""
        variants = emptyList()
//        selectedCategory = null
        editingItem = null
        showCreateItemDialog = false // Add this
        showEditDialog = false
        error = null
    }


    fun startEditItem(item: MenuItem) {
        editingItem = item
        showEditDialog = true
        showCreateItemDialog = false // Ensure create dialog is closed
        // Populate form fields
        itemName = item.name
        description = item.description ?: ""
        variants = item.variants
        currentCategoryId = item.categoryId
    }
    fun updateItem() {
        coroutineScope.launch {
            try {
                val currentItem = editingItem ?: return@launch
                val updatedItem = currentItem.copy(
                    name = itemName,
                    description = description.ifBlank { null },
                    variants = variants
                )

                menuRepo.updateItem(updatedItem)
                // Refresh items list
                loadItems(currentCategoryId?: return@launch)
                error = null
                clearForm()
                showCreateItemDialog = false
                showEditDialog = false
                loadItems(currentCategoryId!!)
            } catch (e: Exception) {
                error = "Failed to update item: ${e.message}"
            }
        }
    }

    fun deleteItem(itemId: Int) {
        coroutineScope.launch {
            try {
                // 1. Delete from database
                menuRepo.deleteItem(itemId)

                // 2. Find and update the correct category
                val categoryEntry = categoryItems.entries.find { (_, items) ->
                    items.any { it.id == itemId }
                }

                // 3. Update local state for specific category
                categoryEntry?.let { (categoryId, items) ->
                    categoryItems[categoryId] = items.filter { it.id != itemId }
                }
            } catch (e: Exception) {
                error = "Delete failed: ${e.message}"
            }
        }
    }

    fun toggleActive(itemId: Int) {
        coroutineScope.launch {
            try {
                // 1. Update database
                menuRepo.toggleItemActive(itemId)

                // 2. Find which category contains the item
                val categoryEntry = categoryItems.entries.find { entry ->
                    entry.value.any { it.id == itemId }
                }

                // 3. Update local state
                categoryEntry?.let { (categoryId, items) ->
                    val updatedItems = items.map { item ->
                        if (item.id == itemId) item.copy(isActive = !item.isActive) else item
                    }
                    categoryItems[categoryId] = updatedItems
                }
            } catch (e: Exception) {
                error = "Toggle failed: ${e.message}"
            }
        }
    }

}