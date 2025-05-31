package presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.model.Category
import data.repository.CategoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CategoryViewModel : KoinComponent {
    private val repository: CategoryRepository by inject()
    private val coroutineScope = CoroutineScope(Dispatchers.Main.immediate)

    var categories by mutableStateOf(emptyList<Category>())
        private set
    var loading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)

    var showCreateDialog by mutableStateOf(false)
    var newCategoryName by mutableStateOf("")
    var categoryError by mutableStateOf<String?>(null)


    var showEditDialog by mutableStateOf(false)
    var editingCategory by mutableStateOf<Category?>(null)
    var editCategoryName by mutableStateOf("")
    var editCategoryError by mutableStateOf<String?>(null)

    init {
        loadCategories()
    }

    fun loadCategories() {
        coroutineScope.launch {
            try {
                val newCategories = repository.getAllCategories()
                categories = newCategories
                error = null
            } catch (e: Exception) {
                error = "Failed to load categories: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    fun createCategory() {
        coroutineScope.launch {
            try {
                if (newCategoryName.isBlank()) {
                    categoryError = "Category name cannot be empty"
                    return@launch
                }

                if (repository.categoryExists(newCategoryName)) {
                    categoryError = "Category already exists!"
                    return@launch
                }

                repository.createCategory(Category(name = newCategoryName))
                loadCategories() // Refresh list
                showCreateDialog = false
                categoryError = null
            } catch (e: Exception) {
                categoryError = "Failed to create category: ${e.message}"
            }
        }
    }

    fun deleteCategory(categoryId: Int) {
        coroutineScope.launch {
            try {
                repository.deleteCategory(categoryId)
                categories = categories.filter {
                    it.id != categoryId
                }
                error = null
            } catch (e: Exception) {
                error = "Delete failed: ${e.message}"
            }
        }
    }

    fun showCreateCategoryDialog() {
        showCreateDialog = true
        newCategoryName = ""
        categoryError = null
    }

    fun showEditCategoryDialog(category: Category) {
        editingCategory = category
        editCategoryName = category.name
        showEditDialog = true
        editCategoryError = null
    }

    fun updateCategory() {
        coroutineScope.launch {
            try {
                val currentCategory = editingCategory ?: return@launch
                if (editCategoryName.isBlank()) {
                    editCategoryError = "Category name cannot be empty"
                    return@launch
                }

                if (repository.categoryExists(editCategoryName)) {
                    editCategoryError = "Category already exists!"
                    return@launch
                }

                repository.updateCategory(currentCategory.copy(name = editCategoryName))
                loadCategories() // Refresh list
                showEditDialog = false
                editCategoryError = null
            } catch (e: Exception) {
                editCategoryError = "Update failed: ${e.message}"
            }
        }
    }
}