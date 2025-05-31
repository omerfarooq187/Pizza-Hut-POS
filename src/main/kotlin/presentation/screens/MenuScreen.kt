package presentation.screens

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.model.Category
import data.model.DiscountType
import data.model.ItemVariant
import data.model.MenuItem
import org.koin.compose.koinInject
import presentation.theme.PizzaCheese
import presentation.theme.PizzaCrust
import presentation.theme.PizzaRed
import presentation.theme.PizzaSauce
import presentation.viewmodel.CategoryViewModel
import presentation.viewmodel.MenuItemViewModel

@Composable
fun CombinedMenuScreen() {
    val categoryViewModel: CategoryViewModel = koinInject()
    val menuItemViewModel: MenuItemViewModel = koinInject()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { categoryViewModel.showCreateCategoryDialog() },
                icon = { Icon(Icons.Default.Add, "Add Category", tint = Color.White) },
                text = { Text("New Category", color = Color.White) },
                containerColor = PizzaRed,
                modifier = Modifier.padding(16.dp)
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        val listState = rememberLazyListState()

        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    items(categoryViewModel.categories) { category ->
                        CategorySection(
                            category = category,
                            menuItemViewModel = menuItemViewModel,
                            onEditCategory = { categoryViewModel.showEditCategoryDialog(category) },
                            onDeleteCategory = { categoryViewModel.deleteCategory(category.id) }
                        )
                        Divider(
                            color = Color.LightGray,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // Scrollbar
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(listState),
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(8.dp)
                )
            }
        }

        // Dialogs
        CategoryCreationDialog(
            showDialog = categoryViewModel.showCreateDialog,
            categoryName = categoryViewModel.newCategoryName,
            errorMessage = categoryViewModel.categoryError,
            onNameChange = { categoryViewModel.newCategoryName = it },
            onConfirm = { categoryViewModel.createCategory() },
            onDismiss = { categoryViewModel.showCreateDialog = false }
        )

        CategoryEditDialog(
            showDialog = categoryViewModel.showEditDialog,
            categoryName = categoryViewModel.editCategoryName,
            errorMessage = categoryViewModel.editCategoryError,
            onNameChange = { categoryViewModel.editCategoryName = it },
            onConfirm = { categoryViewModel.updateCategory() },
            onDismiss = { categoryViewModel.showEditDialog = false }
        )

        if (menuItemViewModel.showEditDialog || menuItemViewModel.showCreateItemDialog) {
            MenuItemDialog(
                viewModel = menuItemViewModel,
                categories = categoryViewModel.categories,
                selectedCategoryId = menuItemViewModel.currentCategoryId
            )
        }
    }
}

@Composable
private fun CategorySection(
    category: Category,
    menuItemViewModel: MenuItemViewModel,
    onEditCategory: () -> Unit,
    onDeleteCategory: () -> Unit
) {
    val items by remember { derivedStateOf { menuItemViewModel.categoryItems[category.id] ?: emptyList() } }
    val error by remember { derivedStateOf { menuItemViewModel.errors[category.id] } }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(category.id) {
        if (items.isEmpty()) {
            menuItemViewModel.loadItems(category.id)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Menu Item") },
            text = { Text("Are you sure you want to delete this menu item?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteCategory()
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }


    Column(modifier = Modifier.fillMaxWidth()) {
        // Modified Category Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PizzaCrust)
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.headlineMedium.copy(color = PizzaRed),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Row {
                IconButton(
                    onClick = onEditCategory,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        "Edit Category",
                        tint = PizzaRed
                    )
                }
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        "Delete Category",
                        tint = PizzaRed
                    )
                }
            }
        }


        Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
//                    .padding(horizontal = 0.dp)
            ) {
                when {
                    menuItemViewModel.loading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center))

                    menuItemViewModel.error != null -> Text(
                        menuItemViewModel.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp))

                    else -> // Usage in Grid (5-6 items per row)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 150.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(items) { item ->
                                    MenuItemCard(
                                        item = item,
                                        onEdit = { menuItemViewModel.startEditItem(item) },
                                        onToggle = { menuItemViewModel.toggleActive(item.id) },
                                        onDelete = {
                                            menuItemViewModel.deleteItem(item.id)
                                        }
                                    )
                                }

                                item {
                                    // Add Item Card
                                    Card(
                                        modifier = Modifier
                                            .width(150.dp)
                                            .height(180.dp)
                                            .padding(4.dp)
                                            .clickable {
                                                menuItemViewModel.apply {
                                                    currentCategoryId = category.id
                                                    showCreateItemDialog = true
                                                }
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = PizzaCrust.copy(alpha = 0.9f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add item",
                                                tint = PizzaRed,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Add New",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = PizzaRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                }
            }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MenuItemCard(
    item: MenuItem,
    onEdit: () -> Unit,
    onToggle: (Int) -> Unit,
    onDelete: () -> Unit
) {
    var showMenuDeleteDialog by remember { mutableStateOf(false) }

    if (showMenuDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showMenuDeleteDialog = false },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete this category?") },
            confirmButton = {
                TextButton(onClick = {
                    showMenuDeleteDialog = false
                    onDelete()
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMenuDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .width(170.dp)
            .height(190.dp)
            .padding(6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = PizzaRed.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with name and switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = item.isActive,
                    onCheckedChange = { onToggle(item.id) },
                    modifier = Modifier.size(36.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PizzaCrust,
                        uncheckedThumbColor = Color.LightGray,
                        uncheckedTrackColor = Color.DarkGray
                    )
                )
            }

            // Image/Icon Section with dynamic icon
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PizzaCheese)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(item.name),
                    contentDescription = "Item image",
                    tint = PizzaSauce,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Actions Row (Price chip and Edit/Delete icons)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                item.variants.firstOrNull()?.let { variant ->
                    Chip(
                        onClick = { /* Handle variant selection */ },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = PizzaCrust,
                            contentColor = PizzaSauce
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Rs. ${variant.price}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit item",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { showMenuDeleteDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete item",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun getCategoryIcon(itemName: String): ImageVector {
    val lowerName = itemName.lowercase()
    return when {
        "pizza" in lowerName -> Icons.Default.LocalPizza
        "burger" in lowerName -> Icons.Default.Fastfood  // burger icon not available, use fastfood
        "ice cream" in lowerName || "icecream" in lowerName -> Icons.Default.Icecream
        "drink" in lowerName || "beverage" in lowerName -> Icons.Default.LocalDrink
        "dessert" in lowerName || "cake" in lowerName -> Icons.Default.Cake
        else -> Icons.Default.Restaurant  // generic dining icon
    }
}


@Composable
fun MenuItemDialog(
    viewModel: MenuItemViewModel,
    categories: List<Category>,
    selectedCategoryId: Int?
) {
    val currentCategory = categories.find { it.id == selectedCategoryId }
    var showCategoryError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { viewModel.clearForm() },
        title = { Text(if (viewModel.showEditDialog) "Edit Menu Item" else "New Menu Item") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                if (showCategoryError) {
                    Text(
                        "Please select a category first!",
                        color = MaterialTheme.colorScheme.error)
                }
                currentCategory?.let {
                    Text(
                        text = "Category: ${it.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                categories.find { it.id == selectedCategoryId }?.name?.let {
                    Text(
                        text = it
                    )
                }

                OutlinedTextField(
                    value = viewModel.itemName,
                    onValueChange = { viewModel.itemName = it },
                    label = { Text("Item Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = viewModel.description,
                    onValueChange = { viewModel.description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                VariantManager(
                    variants = viewModel.variants,
                    onVariantsChanged = { viewModel.variants = it }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    RadioButton(
                        selected = viewModel.discountType == DiscountType.PERCENTAGE,
                        onClick = { viewModel.discountType = DiscountType.PERCENTAGE }
                    )
                    Text(
                        "Percentage",
                        modifier = Modifier.clickable { viewModel.discountType = DiscountType.PERCENTAGE }
                    )
                    Spacer(Modifier.width(16.dp))
                    RadioButton(
                        selected = viewModel.discountType == DiscountType.FIXED,
                        onClick = { viewModel.discountType = DiscountType.FIXED }
                    )
                    Text(
                        "Fixed",
                        modifier = Modifier.clickable { viewModel.discountType = DiscountType.FIXED }
                    )
                }

                // Discount Value Input
                OutlinedTextField(
                    value = viewModel.discountValue.toString(),
                    onValueChange = {
                        if (it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            viewModel.discountValue = it.toDouble()
                        }
                    },
                    label = {
                        when (viewModel.discountType) {
                            DiscountType.PERCENTAGE -> "Discount Percentage"
                            DiscountType.FIXED -> "Discount Amount"
                        }

                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = {
                        if (viewModel.discountType == DiscountType.FIXED) {
                            "Rs. "
                        }
                    },
                    suffix = {
                        if (viewModel.discountType == DiscountType.PERCENTAGE) {
                            "%"
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (viewModel.currentCategoryId == null) {
                        showCategoryError = true
                    } else {
                        showCategoryError = false
                        if (viewModel.showEditDialog) viewModel.updateItem()
                        else viewModel.saveItem()
                    }
                }
            ) {
                Text(if (viewModel.showEditDialog) "Update" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.clearForm() }) {
                Text("Cancel")
            }
        }
    )
}


@Composable
fun VariantManager(
    variants: List<ItemVariant>,
    onVariantsChanged: (List<ItemVariant>) -> Unit
) {
    Column {
        Text("Variants", style = MaterialTheme.typography.headlineSmall)

        variants.forEachIndexed { index, variant ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = variant.size,
                    onValueChange = { newSize ->
                        val updated = variants.toMutableList()
                        updated[index] = variant.copy(size = newSize)
                        onVariantsChanged(updated)
                    },
                    label = { Text("Size") },
                    modifier = Modifier.width(100.dp)
                )

                OutlinedTextField(
                    value = variant.price.toString(),
                    onValueChange = { newPrice ->
                        val updated = variants.toMutableList()
                        updated[index] = variant.copy(price = newPrice.toDoubleOrNull() ?: 0.0)
                        onVariantsChanged(updated)
                    },
                    label = { Text("Price") },
                    modifier = Modifier.width(150.dp)
                )

                IconButton(onClick = {
                    val updated = variants.toMutableList()
                    updated.removeAt(index)
                    onVariantsChanged(updated)
                }) {
                    Icon(Icons.Default.Delete, "Remove variant")
                }
            }
        }

        Button(onClick = {
            onVariantsChanged(variants + ItemVariant(size = "", price = 0.0))
        }) {
            Text("Add Variant")
        }
    }
}
