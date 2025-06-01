package presentation.screens

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import data.model.MenuItemWithVariants
import data.model.OrderItem
import database.OrderType
import org.koin.compose.koinInject
import presentation.theme.PizzaCheese
import presentation.theme.PizzaRed
import presentation.theme.PizzaSauce
import presentation.viewmodel.OrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen( orderId: Int? = null,onFinalize: () -> Unit) {
    val viewModel: OrderViewModel = koinInject()
    val state by viewModel.state
    val snackbarHostState = remember { SnackbarHostState() }


    LaunchedEffect(orderId) {
        if (orderId == null) {
            viewModel.clearOrder()
        } else {
            viewModel.handleEditOrder(orderId)
        }
    }

    // Consolidated alert handling
    LaunchedEffect(viewModel.alerts) {
        viewModel.alerts.lastOrNull()?.let { alert ->
            val result = snackbarHostState.showSnackbar(
                message = alert,
                duration = if (alert.startsWith("Order")) SnackbarDuration.Short
                else SnackbarDuration.Long
            )

            // Clear the alert after showing
            viewModel.alerts = viewModel.alerts - alert

            // Handle completion actions
            if (alert.startsWith("Order updated") || alert.startsWith("Order completed")) {
                onFinalize()
                viewModel.clearOrder()
            }
            viewModel.alerts = emptyList()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (orderId == null) "New Order" else "Edit Order",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // -- Inner Column with form + MenuGrid --
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // First Row: Search and Member Code
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            CustomSearchBar(viewModel)
                        }
                        Box(modifier = Modifier.width(180.dp)) {
                            MemberCodeInput(viewModel)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Second Row: Order Type and Service Charge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OrderTypeSelector(viewModel)
                        }
                        Box(modifier = Modifier.width(140.dp)) {
                            ServiceChargeInput(viewModel)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    MenuGrid(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (state.items.isNotEmpty()) {
                OrderSummarySection(
                    viewModel = viewModel,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}


@Composable
private fun MenuGrid(viewModel: OrderViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state
    val gridState = rememberLazyGridState()

    val groupedItems = state.filteredItems
        .groupBy { it.categoryName }
        .flatMap { (category, items) ->
            listOf(ListItem.Header(category)) + items.map { ListItem.Item(it) }
        }

    Box(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                state = gridState,
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = groupedItems,
                    key = { item ->
                        when (item) {
                            is ListItem.Header -> "header_${item.title}"
                            is ListItem.Item -> "item_${item.menuItemWithVariants.item.id}_${item.menuItemWithVariants.variants.joinToString { it.id.toString() }}"
                        }
                    },
                    span = { item ->
                        if (item is ListItem.Header) {
                            GridItemSpan(maxLineSpan)
                        } else {
                            GridItemSpan(1)
                        }
                    }
                ) { item ->
                    when (item) {
                        is ListItem.Header -> {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier
                                    .padding(vertical = 8.dp, horizontal = 16.dp)
                                    .fillMaxWidth()
                            )
                        }
                        is ListItem.Item -> {
                            MenuItemCard(
                                menuItemWithVariants = item.menuItemWithVariants,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(gridState),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
            )
        }
    }
}

@Composable
private fun MemberCodeInput(viewModel: OrderViewModel) {
    var code by remember { mutableStateOf("") }

    OutlinedTextField(
        value = code,
        onValueChange = {
            if (it.length <= 4 && it.all(Char::isDigit)) {
                code = it
                if (it.length == 4) {
                    viewModel.validateMember(it)
                }
            }
        },
        label = { Text("Member Code") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        trailingIcon = {
            if (viewModel.state.value.isMember) {
                Icon(Icons.Default.Verified, contentDescription = "Valid Member", tint = Color.Green)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}


@Composable
private fun CustomSearchBar(viewModel: OrderViewModel) {
    var query by remember { mutableStateOf("") }

    OutlinedTextField(
        value = query,
        onValueChange = {
            query = it
            viewModel.updateSearchQuery(it)
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { query = "" }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        },
        placeholder = { Text("Search menu items...") },
        shape = MaterialTheme.shapes.extraLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(autoCorrect = false)
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuItemCard(
    menuItemWithVariants: MenuItemWithVariants,
    viewModel: OrderViewModel
) {
    var showVariants by remember { mutableStateOf(false) }
    val item = menuItemWithVariants.item
    val categoryName = menuItemWithVariants.categoryName
    val variants = menuItemWithVariants.variants
    val totalQuantity = variants.sumOf { viewModel.getQuantity(item.id, it.id) }

    // Icon based on category
    val icon = remember(categoryName) {
        when {
            categoryName.contains("pizza", ignoreCase = true) -> Icons.Default.LocalPizza
            categoryName.contains("burger", ignoreCase = true) -> Icons.Default.Fastfood
            categoryName.contains("roll", ignoreCase = true) -> Icons.Default.LunchDining
            categoryName.contains("ice cream", ignoreCase = true) -> Icons.Default.Icecream
            else -> Icons.Default.LocalDining
        }
    }

    Card(
        modifier = Modifier
            .width(170.dp)
            .height(190.dp)
            .padding(6.dp)
            .clickable { showVariants = true },
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PizzaRed.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Item name + badge with quantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (totalQuantity > 0) {
                    Badge(
                        containerColor = Color.White.copy(alpha = 0.9f),
                        contentColor = PizzaRed,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("$totalQuantity")
                    }
                }
            }

            // Icon box
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PizzaCheese)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Category Icon",
                    tint = PizzaSauce,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Price and variants info
            Text(
                text = "From Rs.${variants.minOf { it.price }}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }

    // Variant selection dialog
    if (showVariants) {
        AlertDialog(
            onDismissRequest = { showVariants = false },
            title = { Text("Select Size", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    variants.forEach { variant ->
                        val quantity = viewModel.getQuantity(item.id, variant.id)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.addItem(menuItemWithVariants, variant)
                                    showVariants = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        variant.size,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        "Rs.${variant.price}" + if (variant.memberPrice != null)
                                            " / Rs.${variant.memberPrice} (Member)" else "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                                if (quantity > 0) {
                                    Text(
                                        "$quantity",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = PizzaRed
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVariants = false }) {
                    Text("Close")
                }
            }
        )
    }
}


@Composable
fun OrderSummarySection(
    viewModel: OrderViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val boxWidth = maxWidth
        val boxHeight = maxHeight
        val cardWidth = 300.dp
        val cardHeight = 360.dp // estimated height of summary card

        // Convert Dp to Px
        val density = LocalDensity.current
        val initialOffsetX = with(density) { (boxWidth - cardWidth).toPx() }
        val initialOffsetY = with(density) { (boxHeight - cardHeight).toPx() }

        val offsetX = remember { mutableFloatStateOf(initialOffsetX) }
        val offsetY = remember { mutableFloatStateOf(initialOffsetY) }

        val draggableModifier = Modifier
            .offset { IntOffset(offsetX.value.toInt(), offsetY.value.toInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX.value += dragAmount.x
                    offsetY.value += dragAmount.y
                }
            }

        Card(
            modifier = draggableModifier
                .width(cardWidth)
                .wrapContentHeight(),
            elevation = CardDefaults.cardElevation(12.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Order Summary", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                LazyColumn(modifier = Modifier.heightIn(max = 140.dp)) {
                    items(state.items.values.toList(), key = { "${it.itemId}-${it.variantId}" }) { item ->
                        OrderItemRow(item) { viewModel.removeItem(item) }
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PriceRow("Subtotal:", state.total + state.discount)
                    if (state.isMember) {
                        PriceRow("Member Discount:", -state.discount)
                    }
                    PriceRow("Total:", state.total, style = MaterialTheme.typography.titleMedium, color = PizzaRed)
                }
                FilledTonalButton(
                    onClick = { viewModel.finalizeOrder() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = PizzaRed, contentColor = Color.White),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(imageVector = Icons.Default.DoneAll, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Finalize Order")
                }
                Text(
                    "${state.items.size} Items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PriceRow(label: String, amount: Double, style: TextStyle = MaterialTheme.typography.bodyLarge, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = style, color = color)
        Text(
            "Rs.${"%.2f".format(amount)}",
            style = style,
            color = color
        )
    }
}

@Composable
private fun OrderItemRow(
    item: OrderItem,
    onRemove: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.itemName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                item.variantSize,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${item.quantity} x Rs.${"%.2f".format(item.price)}",
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove item",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderTypeSelector(viewModel: OrderViewModel) {
    val currentOrderType = viewModel.state.value.orderType
    var showDeliveryDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OrderType.entries.forEach { type ->
            FilterChip(
                selected = currentOrderType == type,
                onClick = { viewModel.updateOrderType(type) },
                label = { Text(type.name) },
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = { showDeliveryDialog = true },
            enabled = currentOrderType == OrderType.DELIVERY,
            modifier = Modifier.width(140.dp)
        ) {
            Icon(Icons.Default.DeliveryDining, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Delivery Info")
        }
    }

    if (showDeliveryDialog) {
        DeliveryDetailsDialog(
            viewModel = viewModel,
            onDismiss = { showDeliveryDialog = false }
        )
    }
}


@Composable
private fun DeliveryDetailsDialog(
    viewModel: OrderViewModel,
    onDismiss: () -> Unit
) {
    var rider by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delivery Information") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rider,
                    onValueChange = { rider = it },
                    label = { Text("Rider Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Delivery Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Customer Phone Number") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button({
                viewModel.updateDeliveryDetails(rider, address, phone)
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ServiceChargeInput(viewModel: OrderViewModel) {

    OutlinedTextField(
        value = viewModel.state.value.serviceCharges.toString(),
        onValueChange = { input ->
            if (input.isEmpty() || input.matches(Regex("\\d+"))) {
                val charges = input.toIntOrNull() ?: 0
                viewModel.updateServiceCharges(charges)
            }
        },
        label = { Text(text = if (viewModel.state.value.orderType == OrderType.DELIVERY) "Delivery Charges" else "Service Charges") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        suffix = { Text("Rs.") },
        modifier = Modifier.fillMaxWidth()
    )
}




sealed class ListItem {
    data class Header(val title: String) : ListItem()
    data class Item(val menuItemWithVariants: MenuItemWithVariants) : ListItem()
}