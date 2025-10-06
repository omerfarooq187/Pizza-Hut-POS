package presentation.screens

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import data.model.RawItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import presentation.theme.PizzaRed
import presentation.theme.PizzaWhite
import presentation.viewmodel.InventoryViewModel

// presentation/screens/InventoryScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: InventoryViewModel = koinInject()) {
    var showAddDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Inventory Management", style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    IconButton(onClick = { viewModel.loadInventory() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, "Add Item") },
                text = { Text("New Item") },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Status Summary
            InventorySummary(viewModel)


            if (viewModel.errors.isNotEmpty()) {
                ErrorMessages(viewModel.errors)

                LaunchedEffect(viewModel.errors) {
                    // Wait 5 seconds, then clear errors
                    delay(2000)
                    viewModel.clearErrors()
                }
            }

            // Inventory List
            when {
                viewModel.inventoryItems.isEmpty() -> EmptyInventory()
                else -> InventoryList(viewModel.inventoryItems, viewModel)
            }
        }

        AddRawItemDialog(
            showDialog = showAddDialog,
            onDismiss = { showAddDialog = false },
            onCreate = viewModel::createRawItem
        )
    }

    if (viewModel.showReplenishDialog) {
        ReplenishDialog(
            item = viewModel.selectedItem,
            onDismiss = viewModel::hideReplenishDialog,
            onConfirm = viewModel::replenishStock
        )
    }

    if (viewModel.showEditDialog && viewModel.selectedItemForEdit != null) {
        EditRawItemDialog(
            showDialog = viewModel.showEditDialog,
            item = viewModel.selectedItemForEdit,
            onDismiss = { viewModel.showEditDialog = false },
            onConfirm = viewModel::updateRawItem
        )
    }

    if (viewModel.showDeleteDialog && viewModel.itemToDelete != null) {
        DeleteConfirmationDialog(
            showDialog = viewModel.showDeleteDialog,
            item = viewModel.itemToDelete,
            onDismiss = viewModel::dismissDeleteDialog,
            onConfirm = viewModel::deleteRawItem
        )
    }


}

@Composable
private fun InventorySummary(viewModel: InventoryViewModel) {
    val lowStockCount by remember(viewModel.lowStockAlerts) {
        derivedStateOf { viewModel.lowStockAlerts.size }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = viewModel.inventoryItems.size.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("Total Items", style = MaterialTheme.typography.bodySmall)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = lowStockCount.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = if (lowStockCount > 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
                Text("Low Stock", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ErrorMessages(errors: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        errors.forEach { error ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }

}

@Composable
private fun EmptyInventory() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Inventory,
                contentDescription = "Empty",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Inventory Items Found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "Click the + button to add new items",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun InventoryList(items: List<RawItem>, viewModel: InventoryViewModel) {
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    InventoryItemCard(item, viewModel)
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
}

@Composable
private fun InventoryItemCard(item: RawItem, viewModel: InventoryViewModel) {
    val currentItem by rememberUpdatedState(item)
    val isValidThreshold = item.alertThreshold?.let { it > 0 } ?: false

    val progress = remember(item) {
        if (isValidThreshold) (item.currentStock / item.alertThreshold!!).coerceAtMost(1.0).toFloat()
        else 1f
    }

    val color = when {
        item.alertThreshold != null && item.currentStock <= item.alertThreshold -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Row {
                    IconButton(onClick = { viewModel.showReplenishDialog(currentItem) }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add Stock", tint = PizzaRed)
                    }
                    IconButton(onClick = {
                        viewModel.selectedItemForEdit = item
                        viewModel.showEditDialog = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Item", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.confirmDelete(item) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Item", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Current Stock", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text("${formatStockAmount(item.currentStock)} ${item.unit}", style = MaterialTheme.typography.bodyLarge)
                }

                item.alertThreshold?.let {
                    Column {
                        Text("Alert Threshold", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text("$it ${item.unit}", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            StockStatusIndicator(item.currentStock, item.alertThreshold)
        }
    }
}

private fun formatStockAmount(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
        amount.toInt().toString()
    } else {
        String.format("%.2f", amount).trimEnd('0').trimEnd('.')
    }
}

@Composable
private fun StockStatusIndicator(
    currentStock: Double,
    alertThreshold: Double?
) {
    val (text, color, icon) = when {
        alertThreshold == null -> Triple("No threshold set", MaterialTheme.colorScheme.outline, Icons.Default.Info)
        currentStock < alertThreshold -> Triple("Low Stock", MaterialTheme.colorScheme.error, Icons.Default.Warning)
        else -> Triple("In Stock", MaterialTheme.colorScheme.primary, Icons.Default.CheckCircle)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium
        )
    }
}



@Composable
fun AddRawItemDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onCreate: (RawItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var currentStock by remember { mutableStateOf("") }
    var alertThreshold by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add New Raw Item") },
            text = {
                Column(modifier = Modifier.padding(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name*") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (kg, liters, etc.)*") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = currentStock,
                        onValueChange = { currentStock = it },
                        label = { Text("Initial Stock*") },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = alertThreshold,
                        onValueChange = { alertThreshold = it },
                        label = { Text("Low Stock Alert Threshold") },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = supplier,
                        onValueChange = { supplier = it },
                        label = { Text("Supplier") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newItem = RawItem(
                            name = name,
                            unit = unit,
                            currentStock = currentStock.toDoubleOrNull() ?: 0.0,
                            alertThreshold = alertThreshold.toDoubleOrNull(),
                            supplier = supplier.ifEmpty { null },
                            description = description.ifEmpty { null }
                        )
                        onCreate(newItem)
                        onDismiss()
                    },
                    enabled = name.isNotBlank() && unit.isNotBlank() && currentStock.isNotBlank()
                ) {
                    Text("Add Item")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EditRawItemDialog(
    showDialog: Boolean,
    item: RawItem?,
    onDismiss: () -> Unit,
    onConfirm: (RawItem) -> Unit
) {
    if (item == null) return
    var name by remember { mutableStateOf(item.name) }
    var unit by remember { mutableStateOf(item.unit) }
    var currentStock by remember { mutableStateOf(item.currentStock.toString()) }
    var alertThreshold by remember { mutableStateOf(item.alertThreshold?.toString() ?: "") }
    var supplier by remember { mutableStateOf(item.supplier ?: "") }
    var description by remember { mutableStateOf(item.description ?: "") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Edit Raw Item") },
            text = {
                Column(modifier = Modifier.padding(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name*") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit*") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = currentStock,
                        onValueChange = { currentStock = it },
                        label = { Text("Current Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = alertThreshold,
                        onValueChange = { alertThreshold = it },
                        label = { Text("Alert Threshold") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(value = supplier, onValueChange = { supplier = it }, label = { Text("Supplier") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = item.copy(
                            name = name,
                            unit = unit,
                            currentStock = currentStock.toDoubleOrNull() ?: 0.0,
                            alertThreshold = alertThreshold.toDoubleOrNull(),
                            supplier = supplier.ifEmpty { null },
                            description = description.ifEmpty { null }
                        )
                        onConfirm(updated)
                        onDismiss()
                    },
                    enabled = name.isNotBlank() && unit.isNotBlank()
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
    }
}


@Composable
private fun ReplenishDialog(
    item: RawItem?,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Replenish Stock") },
        text = {
            Column {
                Text("Item: ${item?.name ?: "N/A"}")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount to add (${item?.unit ?: "units"})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    amount.toDoubleOrNull()?.takeIf { it > 0 }?.let(onConfirm)
                },
                enabled = amount.toDoubleOrNull()?.let { it > 0 } ?: false
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
fun DeleteConfirmationDialog(
    showDialog: Boolean,
    item: RawItem?,
    onDismiss: () -> Unit,
    onConfirm: (RawItem) -> Unit
) {
    if (showDialog && item != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete ${item.name}?") },
            text = { Text("This action cannot be undone. Are you sure you want to permanently delete this item?") },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm(item)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}