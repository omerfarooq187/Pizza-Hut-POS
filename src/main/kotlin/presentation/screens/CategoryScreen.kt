package presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import data.model.Category
import org.koin.compose.koinInject
import presentation.viewmodel.CategoryViewModel
import presentation.viewmodel.MenuItemViewModel


// presentation/screen/CategoryScreen.kt
@Composable
fun CategoryScreen(onSelectedCategory:(Int)-> Unit) {
    val viewModel: CategoryViewModel = koinInject()
    Column(modifier = Modifier.padding(16.dp)) {
        // Create New Button
        Button(
            onClick = { viewModel.showCreateCategoryDialog() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red
            )
        ) {
            Text("Create New Category")
        }

        // Add the dialog component
        CategoryCreationDialog(
            showDialog = viewModel.showCreateDialog,
            categoryName = viewModel.newCategoryName,
            errorMessage = viewModel.categoryError,
            onNameChange = { viewModel.newCategoryName = it },
            onConfirm = { viewModel.createCategory() },
            onDismiss = { viewModel.showCreateDialog = false }
        )

        CategoryEditDialog(
            showDialog = viewModel.showEditDialog,
            categoryName = viewModel.editCategoryName,
            errorMessage = viewModel.editCategoryError,
            onNameChange = { viewModel.editCategoryName = it },
            onConfirm = { viewModel.updateCategory() },
            onDismiss = {
                viewModel.showEditDialog = false
                viewModel.editingCategory = null
            }
        )

        // Loading/Error States
        when {
            viewModel.loading -> CircularProgressIndicator()
            viewModel.error != null -> Text(viewModel.error!!, color = Color.Red)
            else -> CategoryList(
                categories = viewModel.categories,
                onDelete = viewModel::deleteCategory,
                onEdit = viewModel::showEditCategoryDialog,
                onSelect = onSelectedCategory
            )
        }
    }
}

@Composable
private fun CategoryList(
    categories: List<Category>,
    onDelete: (Int) -> Unit,
    onEdit: (Category) -> Unit,
    onSelect: (Int) -> Unit
) {
    val menuItemViewModel: MenuItemViewModel = koinInject()
    LazyColumn {
        items(categories) { category ->
            CategoryItem(category, onDelete, onEdit) {
                onSelect(category.id)
            }
        }
    }
}


@Composable
private fun CategoryItem(
    category: Category,
    onDelete: (Int) -> Unit,
    onEdit: (Category) -> Unit,
    onClick: ()-> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .clickable {
                    onClick()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(category.name, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { onEdit(category) }) {
                Icon(Icons.Default.Edit, "Edit")
            }
            IconButton(onClick = { onDelete(category.id) }) {
                Icon(Icons.Default.Delete, "Delete")
            }
        }
    }
}

@Composable
fun CategoryCreationDialog(
    showDialog: Boolean,
    categoryName: String,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Create New Category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = onNameChange,
                        label = { Text("Category Name") },
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    enabled = categoryName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun CategoryEditDialog(
    showDialog: Boolean,
    categoryName: String,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Edit Category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = onNameChange,
                        label = { Text("Category Name") },
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    enabled = categoryName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}