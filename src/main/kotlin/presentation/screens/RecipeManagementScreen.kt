package presentation.screens

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import data.model.ItemVariant
import data.model.MenuItem
import data.model.RawItem
import data.model.Recipe
import org.koin.compose.koinInject
import presentation.viewmodel.RecipeViewModel

@Composable
fun RecipeManagementScreen(viewModel: RecipeViewModel = koinInject()) {
    // Load recipes when screen appears
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // Error messages
        if (viewModel.errors.isNotEmpty()) {
            ErrorMessages(viewModel.errors)
        }

        // Input Section
        RecipeInputSection(viewModel)

        // Recipe List
        RecipeListSection(viewModel)
    }
}

@Composable
private fun RecipeInputSection(viewModel: RecipeViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text("Add New Recipe", style = MaterialTheme.typography.headlineMedium)

        DropdownMenuComponent(
            label = "Select Menu Item Variant",
            items = viewModel.menuItemsWithVariants.flatMap { it.variants },
            itemToString = { "${it.size} (${it.itemName})" },
            onItemSelected = { viewModel.selectedVariant = it }
        )

        DropdownMenuComponent(
            label = "Select Ingredient",
            items = viewModel.rawItems,
            itemToString = { it.name },
            onItemSelected = { viewModel.selectedRawItem = it }
        )

        OutlinedTextField(
            value = viewModel.quantity,
            onValueChange = { viewModel.quantity = it },
            label = { Text("Quantity Needed") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (viewModel.validateForm()) {
                    viewModel.addRecipe()
                }
            },
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
        ) {
            Text("Add Recipe")
        }
    }
}

@Composable
private fun RecipeListSection(viewModel: RecipeViewModel) {
    val recipes by remember { derivedStateOf {
        viewModel.menuItemsWithVariants.flatMap { menuItem ->
            menuItem.variants.flatMap { variant ->
                variant.recipes?.map { recipe ->
                    Triple(menuItem.item, variant, recipe)
                } ?: emptyList()
            }
        }
    }}

    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (recipes.isEmpty()) {
                    item {
                        Text(
                            "No recipes found",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                items(recipes) { (menuItem, variant, recipe) ->
                    RecipeCard(
                        menuItem = menuItem,
                        variant = variant,
                        recipe = recipe,
                        rawItem = viewModel.rawItems.find { it.id == recipe.rawItemId },
                        onDelete = {
                            viewModel.deleteRecipe(variant.id, recipe.rawItemId)
                        }
                    )
                    Divider()
                }
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
            )
        }
    }
}

@Composable
private fun RecipeCard(
    menuItem: MenuItem,
    variant: ItemVariant,
    recipe: Recipe,
    rawItem: RawItem?,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        menuItem.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Variant: ${variant.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete Recipe")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Ingredient: ${rawItem?.name ?: "Unknown"}")
            Text("Quantity: ${recipe.quantityNeeded} ${rawItem?.unit ?: ""}")
        }
    }
}

@Composable
private fun ErrorMessages(errors: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .background(MaterialTheme.colorScheme.errorContainer)
    ) {
        errors.forEach { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun <T> DropdownMenuComponent(
    label: String,
    items: List<T>,
    itemToString: (T) -> String,
    onItemSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<T?>(null) }

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedItem?.let { itemToString(it) } ?: label)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth() // Add width modifier
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemToString(item)) },
                    onClick = {
                        selectedItem = item
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}