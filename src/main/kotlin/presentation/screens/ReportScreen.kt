package presentation.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import data.model.Order
import data.model.OrderStatus
import data.repository.Quadruple
import database.OrderType
import org.joda.time.format.DateTimeFormat
import org.koin.compose.koinInject
import presentation.viewmodel.ReportViewModel


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportScreen(onEditOrder: (Int) -> Unit) {
    val viewModel: ReportViewModel = koinInject()
    val orders = viewModel.orders


    val memberCodeFilter = viewModel.memberCodeFilter
    val memberFilters = listOf("All", "Member", "Non-Member")
    val memberFilterError by remember { derivedStateOf { viewModel.memberFilterError } }

    // Observe ViewModel's parameters
    val selectedRange = viewModel.selectedRange
    val selectedMemberFilter = viewModel.selectedMemberFilter
    val selectedStatus = viewModel.selectedStatus

    LaunchedEffect(
        viewModel.selectedRange,
        viewModel.selectedMemberFilter,
        viewModel.memberCodeFilter,
        viewModel.selectedStatus
    ) {
        viewModel.filterOrders()
    }
    LaunchedEffect(Unit) {
        viewModel.refreshOrders() // This will load both orders and member stats
    }


    // Member statistics
    val (memberOrderCount, totalOrders, memberPercentage) = remember(orders) {
        val memberOrders = orders.filter { it.isMember }
        val total = orders.size
        val count = memberOrders.size
        val percentage = if (total > 0) (count.toFloat() / total * 100) else 0f
        Triple(count, total, percentage)
    }


    if (viewModel.exportMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearMessage() },
            title = { Text("Export Status") },
            text = { Text(viewModel.exportMessage!!) },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearMessage() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFE724C)
                    )
                ) {
                    Text("OK")
                }
            }
        )
    }


    Column(modifier = Modifier.fillMaxSize()) {
        // Enhanced App Bar with Action
        CenterAlignedTopAppBar(
            title = {
                Text("Order Reports", style = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                ))
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            actions = {
                IconButton(onClick = { /* Export functionality */ }) {
                    Icon(Icons.Default.Print, "Export", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        )

        // Filters Section
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .heightIn(max = 280.dp) // Constrain max height
                    .verticalScroll(rememberScrollState())
            ) {
                // Compact filter layout
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date Range Filter
                    FilterSection(
                        title = "Date Range",
                        options = listOf("Today", "This Week", "This Month"),
                        selected = selectedRange,
                        onSelected = { viewModel.selectedRange = it }
                    )

                    // Status Filter
                    FilterSection(
                        title = "Status",
                        options = listOf("ALL") + OrderStatus.entries.map { it.name },
                        selected = selectedStatus,
                        onSelected = { viewModel.selectedStatus = it }
                    )

                    // Member Type Filter
                    FilterSection(
                        title = "Member Type",
                        options = listOf("ALL", "Members", "Non-Member"),
                        selected = selectedMemberFilter,
                        onSelected = { viewModel.selectedMemberFilter = it }
                    )

                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    EnhancedMemberCodeFilter(
                        modifier = Modifier.weight(1.5f),
                        value = memberCodeFilter,
                        onValueChange = { code ->
                            if (code.length <= 4 && code.all { it.isDigit() }) {
                                viewModel.memberCodeFilter = code
                                viewModel.validateMember(code)
                            }
                        },
                        isError = memberFilterError != null,
                        supportingText = memberFilterError,
                        isValidMember = viewModel.isMember
                    )
                    TopMemberDropdown(
                        topMembers = viewModel.topMembers,
                        modifier = Modifier.padding(top = 12.dp).weight(1f)
                    )
                }
                // Member Code Filter

                // Statistics Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CompactStatCard("Total Orders", totalOrders.toString())
                    CompactStatCard("Member %", "%.1f%%".format(memberPercentage))
                }
            }
        }

        // Orders List
        val listState = rememberLazyListState()

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            // LazyColumn with scroll state
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                items(orders.reversed()) { order ->
                    EnhancedOrderRow(order, viewModel, onEditOrder)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            // Vertical scrollbar
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopMemberDropdown(
    topMembers: List<Quadruple<Int, String, String, Long>>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val displayedMembers = remember(topMembers) {
        if (topMembers.isEmpty()) emptyList() else listOf(topMembers.first()) + topMembers.drop(1)
    }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(12.dp)
        ) {
            if (displayedMembers.isEmpty()) {
                Text("No top members", style = MaterialTheme.typography.bodySmall)
            } else {
                // Always show first member
                MemberRow(displayedMembers.first())

                // Show expand indicator if there's more
                if (displayedMembers.size > 1) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Show less" else "Show more",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 200.dp)
        ) {
            displayedMembers.drop(1).forEach { (code, name, count, phone) ->
                DropdownMenuItem(
                    text = {
                        MemberRow(Quadruple(code, name, count, phone))
                    },
                    onClick = { expanded = false }
                )
            }
        }
    }
}

@Composable
private fun MemberRow(member: Quadruple<Int, String, String, Long>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1.5f)) {
            Text(
                "#${member.first.toString().padStart(4, '0')}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                member.second.ifEmpty { "Unknown" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                member.third.ifEmpty { "Unknown" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "${member.fourth} orders",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EnhancedOrderRow(
    order: Order,
    viewModel: ReportViewModel,
    onEditOrder: (Int) -> Unit
) {
    val dateTimeFormatter = DateTimeFormat.forPattern("dd MMM yyyy • hh:mm aa")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "#${order.id}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    dateTimeFormatter.print(order.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Content
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Items Column
                Column(modifier = Modifier.weight(2f)) {

                    Text(
                        "Items (${order.items.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(
                        modifier = Modifier
                            .heightIn(max = 150.dp)
                    ) {
                        order.items.forEach { item ->
                            Text(
                                text = "• ${item.quantity}x ${item.itemName}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }

                // Details Column
                Column(modifier = Modifier.weight(1f)) {
                    OrderStatusDropdown(
                        currentStatus = order.status,
                        onStatusSelected = { newStatus ->
                            viewModel.updateOrderStatus(order.id, newStatus)
                        }
                    )
                    OrderDetailItem("Type", order.orderType.name)
                    OrderDetailItem("Member", if (order.isMember) "#${order.memberId}" else "Guest")
                    OrderDetailItem("Total", "Rs. ${"%.2f".format(order.totalAmount)}")
                }

                // Status & Actions
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    StatusChip(status = order.status)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { onEditOrder(order.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.printReceiptForOrder(order) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Print,
                                contentDescription = "Print",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderStatusDropdown(
    currentStatus: OrderStatus,
    onStatusSelected: (OrderStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        // Custom styled text field
        Box(
            modifier = Modifier
                .menuAnchor()
                .background(
                    color = when (currentStatus) {
                        OrderStatus.COMPLETED -> Color.Green.copy(alpha = 0.2f)
                        OrderStatus.CANCELLED -> Color.Red.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.small
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .wrapContentWidth()
        ) {
            Text(
                text = currentStatus.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = when (currentStatus) {
                        OrderStatus.COMPLETED -> Color.Green
                        OrderStatus.CANCELLED -> Color.Red
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            )
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            OrderStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = {
                        Text(
                            status.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    onClick = {
                        onStatusSelected(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun OrderDetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun EnhancedMemberCodeFilter(
    modifier: Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    supportingText: String?,
    isValidMember: Boolean
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Member Code") },
            prefix = { Text("#", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = {
                when {
                    isValidMember -> Icon(
                        Icons.Default.Verified,
                        "Valid",
                        tint = Color.Green
                    )
                    isError -> Icon(
                        Icons.Default.Error,
                        "Error",
                        tint = Color.Red
                    )
                }
            },
            supportingText = {
                if (isError) {
                    Text(supportingText ?: "", color = Color.Red)
                }
            },
            isError = isError,
            shape = MaterialTheme.shapes.medium,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    title: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelected(option) },
                    label = { Text(option) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        selectedBorderColor = Color.Transparent
                    )
                )
            }
        }
    }
}

@Composable
private fun CompactStatCard(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}