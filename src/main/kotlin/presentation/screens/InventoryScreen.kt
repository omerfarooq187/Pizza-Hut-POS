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
import org.koin.compose.koinInject
import presentation.theme.PizzaRed
import presentation.theme.PizzaWhite
import presentation.viewmodel.InventoryViewModel

// presentation/screens/InventoryScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: InventoryViewModel = koinInject()) {
    var showAddDialog by remember { mutableStateOf(false) }

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

            // Error Messages
            if (viewModel.errors.isNotEmpty()) {
                ErrorMessages(viewModel.errors)
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
    // Check if threshold is valid (positive and not null)
    val isValidThreshold = item.alertThreshold?.let { it > 0 } ?: false
    val currentItem by rememberUpdatedState(item)


    val progress = remember(item) {
        if (isValidThreshold) {
            // Calculate progress as currentStock / threshold, capped at 1.0
            (item.currentStock / item.alertThreshold!!).coerceAtMost(1.0).toFloat()
        } else {
            1f // Full bar if no valid threshold
        }
    }

    // Determine color based on stock status
    val color = when {
        item.currentStock <= item.alertThreshold!! -> MaterialTheme.colorScheme.error
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = { viewModel.showReplenishDialog(currentItem) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PizzaRed,
                            contentColor = PizzaWhite
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Add Stock")
                    }
                }

                StockStatusIndicator(
                    currentStock = item.currentStock,
                    alertThreshold = item.alertThreshold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stock Progress
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
                    Text(
                        text = "Current Stock",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "${formatStockAmount(item.currentStock)} ${item.unit}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                item.alertThreshold.let {
                    Column {
                        Text(
                            text = "Alert Threshold",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "$it ${item.unit}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
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