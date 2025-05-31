package presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.repository.OrderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DashboardViewModel : KoinComponent {

    // Inject the repository
    private val orderRepository: OrderRepository by inject()

    // Coroutine scope (can be customized if needed)
    private val viewModelScope = CoroutineScope(Dispatchers.Main.immediate)

    // StateFlows for UI observation
    var todayOrderCount by mutableStateOf(0L)

    var todayTotalSales by mutableStateOf(0.0)

    // Initialization block
    init {
        fetchDashboardData()
    }

    fun fetchDashboardData() {
        viewModelScope.launch {
            val ordersCount = orderRepository.getTodayTotalOrdersCount()
            val totalSales = orderRepository.getTodayTotalSales()

            todayOrderCount = ordersCount
            todayTotalSales = totalSales
        }
    }
}
