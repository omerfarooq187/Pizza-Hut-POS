package presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.model.RawItem
import data.repository.RawItemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import service.InventoryService

class InventoryViewModel : KoinComponent {
    private val inventoryService: InventoryService by inject()
    private val rawItemRepo: RawItemRepository by inject()

    private val _inventoryItems = mutableStateListOf<RawItem>()
    val inventoryItems: List<RawItem> get() = _inventoryItems

    private val _lowStockAlerts = mutableStateListOf<String>()
    val lowStockAlerts: List<String> get() = _lowStockAlerts

    private val _showReplenishDialog = mutableStateOf(false)
    val showReplenishDialog: Boolean get() = _showReplenishDialog.value

    var selectedItem by mutableStateOf<RawItem?>(null)

    private val _errors = mutableStateListOf<String>()
    val errors: List<String> get() = _errors

    init {
        loadInventory()
    }

    fun loadInventory() {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                val items = rawItemRepo.getAllRawItems()
                val alerts = inventoryService.checkLowStock()

                _inventoryItems.clear()
                _inventoryItems.addAll(items)

                _lowStockAlerts.clear()
                alerts.forEach {
                    _lowStockAlerts.add("${it.name} - ${it.currentStock} ${it.unit} (Alert at ${it.alertThreshold})")
                }
            } catch (e: Exception) {
                _lowStockAlerts.add("Error loading inventory: ${e.message}")
            }
        }
    }

    fun showReplenishDialog(item: RawItem) {
        selectedItem = item
        _showReplenishDialog.value = true
    }

    fun hideReplenishDialog() {
        _showReplenishDialog.value = false
        selectedItem = null
    }

    fun replenishStock(amount: Double) {
        val currentItem = selectedItem ?: run {
            _errors.add("No item selected")
            return
        }

        CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                _errors.clear()
                rawItemRepo.updateStock(
                    rawItemId = currentItem.id,
                    delta = amount,
                    reason = "Manual replenishment"
                )
                _errors.add("Added $amount ${currentItem.unit} to ${currentItem.name}")
                loadInventory()
            } catch (e: Exception) {
                _errors.add("Replenishment failed: ${e.message}")
            } finally {
                hideReplenishDialog()
            }
        }
    }

    fun createRawItem(rawItem: RawItem) {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                rawItemRepo.createRawItem(rawItem)
                _errors.add("Successfully added ${rawItem.name}")
                loadInventory() // Refresh the list
            } catch (e: Exception) {
                _errors.add("Failed to add item: ${e.message}")
            }
        }
    }
}