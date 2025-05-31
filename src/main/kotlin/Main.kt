// Main.kt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import auth.LoginScreen
import di.appModule
import org.koin.core.context.startKoin
import presentation.screens.*
import presentation.theme.PizzaHutTheme
import session.SessionManager

fun main()  {
    application {
        startKoin { modules(appModule) }

        Window(title = "Pizza POS System", onCloseRequest = ::exitApplication, icon = painterResource(resourcePath = "logo.jpg")) {
            PizzaHutTheme {
                var isLoggedIn by remember { mutableStateOf(SessionManager.isLoggedIn()) }

                if (!isLoggedIn) {
                    LoginScreen {
                        isLoggedIn = true
                    }
                } else {
                    TabbedInterface(onLogout = {
                        SessionManager.logout()
                        isLoggedIn = false
                    })
                }
            }
        }
    }
}


@Composable
fun TabbedInterface(onLogout:()-> Unit) {
    var selectedTab by remember { mutableStateOf(Tab.DASHBOARD) }

    var orderToEdit by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(selectedTab) {
        if (selectedTab != Tab.ORDERS) {
            orderToEdit = null
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {

        // Sidebar
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(220.dp)
                .background(Color(0xFFFE724C)),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Image(
                painter = painterResource("logo.jpg"),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(24.dp))

            Tab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                val background = if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent
                val textColor = if (isSelected) Color.White else Color.LightGray
                val iconTint = if (isSelected) Color.White else Color.LightGray

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(background)
                        .clickable { selectedTab = tab }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = tab.icon, contentDescription = tab.title, tint = iconTint)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = tab.title, color = textColor, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Main Content
        Box(modifier = Modifier.fillMaxSize().background(Color.White).padding(16.dp)) {
            when (selectedTab) {
                Tab.DASHBOARD -> DashboardScreen()
                Tab.MENU -> CombinedMenuScreen()
                Tab.ORDERS -> {
                    if (orderToEdit != null) {
                        OrderScreen(
                            orderId = orderToEdit,
                            onFinalize = {
                                orderToEdit = null
//                                selectedTab = Tab.REPORTS // Optional: Switch back after completion
                            }
                        )
                    } else {
                        OrderScreen {
                            // For new orders, stay in Orders tab
                            orderToEdit = null
//                            selectedTab = Tab.REPORTS
                        }
                    }
                }
                Tab.REPORTS -> {
                    ReportScreen { orderId ->
                        // When editing from Reports, switch to Orders tab
                        orderToEdit = orderId
                        selectedTab = Tab.ORDERS
                    }
                }

                Tab.INVENTORY -> {
                    InventoryScreen()
                }
                Tab.RECIPES -> {
                    RecipeManagementScreen()
                }
                Tab.MEMBERS -> {
                    MemberScreen()
                }
                Tab.PROFILE -> {
                    ProfileScreen {
                        onLogout()
                    }
                }
            }
        }
    }
}

// New Tab enum with Dashboard
enum class Tab(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    MENU(title = "Menu", Icons.Default.Menu),
    ORDERS("Orders", Icons.Default.PointOfSale),
    REPORTS("Reports", Icons.Default.Analytics),
    INVENTORY("Inventory", Icons.Default.Inventory),
    RECIPES("Recipes", Icons.Default.RestaurantMenu),
    MEMBERS("Members", Icons.Default.PersonAdd),
    PROFILE("Profile", Icons.Default.Person)
}
