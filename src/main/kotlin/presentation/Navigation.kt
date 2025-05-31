package presentation


import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

class NavController {
    private val _screen = mutableStateOf<Screen>(Screen.List)
    val screen: State<Screen> = _screen
    fun navigateTo(screen: Screen) {
        this._screen.value = screen
    }
}

sealed class Screen {
    object List: Screen()
    object Form: Screen()
}


//@Composable
//fun TabNavigation() {
//    val tabs = listOf("Categories", "Menu Items")
//    var selectedTabIndex by remember { mutableStateOf(0) }
//
//    Column {
//        TabRow(selectedTabIndex = selectedTabIndex) {
//            tabs.forEachIndexed { index, title ->
//                Tab(
//                    selected = selectedTabIndex == index,
//                    onClick = { selectedTabIndex = index },
//                    text = { Text(title) }
//                )
//            }
//        }
//
//        when (selectedTabIndex) {
//            0 -> CategoryScreen()
//            1 -> MenuItemScreen()
//        }
//    }
//
//}
