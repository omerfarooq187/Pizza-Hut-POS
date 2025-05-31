package presentation.screens

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.model.OrderItem
import data.model.OrderStatus
import kotlinx.coroutines.launch
import org.joda.time.format.DateTimeFormat
import org.koin.compose.koinInject
import presentation.theme.PizzaCrust
import presentation.theme.PizzaRed
import presentation.viewmodel.DashboardViewModel
import presentation.viewmodel.ReportViewModel
import java.text.NumberFormat
import java.util.*

@Composable
fun DashboardScreen() {
    val dashboardViewModel: DashboardViewModel = koinInject()
    val reportViewModel: ReportViewModel = koinInject()
    val coroutineScope = rememberCoroutineScope()

    val todayOrders = dashboardViewModel.todayOrderCount
    val todaySales = dashboardViewModel.todayTotalSales
    val recentOrders = reportViewModel.orders.takeLast(20).reversed()

    LaunchedEffect(reportViewModel.orders) {
        reportViewModel.refreshOrders()
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            dashboardViewModel.fetchDashboardData()
            reportViewModel.getAllOrders()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Dashboard Overview",
            style = MaterialTheme.typography.headlineMedium.copy(color = Color(0xFF2C3E50)),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Stats Cards
        Row(modifier = Modifier.fillMaxWidth()) {
            DashboardCard(
                title = "Daily Sales",
                value = "Rs. ${formatPrice(todaySales)}",
                color = PizzaCrust,
                icon = Icons.Default.AttachMoney,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            DashboardCard(
                title = "Total Orders",
                value = todayOrders.toString(),
                color = PizzaRed,
                icon = Icons.Default.ShoppingCart,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Orders Section
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Recent Orders (Last 20)",
                    style = MaterialTheme.typography.titleLarge.copy(color = Color(0xFF2C3E50)),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (recentOrders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No recent orders found", color = Color.Gray)
                    }
                } else {
                    val scrollState = rememberScrollState()

                    Box {
                        Column(
                            modifier = Modifier
                                .verticalScroll(scrollState)
                                .fillMaxHeight()
                                .padding(end = 12.dp) // leave space for scrollbar
                        ) {
                            recentOrders.forEachIndexed { index, order ->
                                OrderRow(
                                    time = order.createdAt.toString(DateTimeFormat.forPattern("hh:mm a")),
                                    items = order.items,
                                    total = formatPrice(order.totalAmount),
                                    status = order.status,
                                    memberId = order.memberId?.toString() ?: "Guest",
                                    orderType = order.orderType.name
                                )
                                if (index < recentOrders.size - 1) {
                                    Divider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }

                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(scrollState),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    value: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
fun OrderRow(
    time: String,
    items: List<OrderItem>,
    total: String,
    status: OrderStatus,
    memberId: String,
    orderType: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Time and Type Column
            Column(modifier = Modifier.weight(1.2f)) {
                Text(time, style = MaterialTheme.typography.labelSmall.copy(color = Color.DarkGray))
                Spacer(modifier = Modifier.height(4.dp))
                Chip(
                    label = orderType,
                    color = when (orderType) {
                        "DELIVERY" -> Color(0xFF2196F3)
                        "TAKEAWAY" -> Color(0xFF4CAF50)
                        else -> Color.Gray
                    }
                )
            }

            // Items Column - Updated for scroll
            Column(
                modifier = Modifier
                    .weight(2.5f)
                    .heightIn(max = 200.dp) // Maximum height before scrolling
                    .verticalScroll(rememberScrollState())
            ) {
                items.forEach { item ->
                    Text(
                        text = "${item.quantity}x ${item.itemName}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            // Member & Total Column
            Column(modifier = Modifier.weight(1.5f), horizontalAlignment = Alignment.End) {
            Text(
                text = if (memberId == "Guest") "Guest" else "Member #$memberId",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF666666))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Rs $total",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
        }

            // Status Column
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            StatusChip(status = status)
        }
        }
    }
}

@Composable
fun StatusChip(status: OrderStatus) {
    val (color, text) = when (status) {
        OrderStatus.COMPLETED -> Color(0xFF4CAF50) to "Completed"
        OrderStatus.PENDING -> Color(0xFFFFC107) to "Pending"
        OrderStatus.CANCELLED -> Color(0xFFF44336) to "Cancelled"
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
fun Chip(label: String, color: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
        )
    }
}

// Improved price formatting
private fun formatPrice(amount: Double): String {
    return NumberFormat.getNumberInstance(Locale.US).format(amount)
}