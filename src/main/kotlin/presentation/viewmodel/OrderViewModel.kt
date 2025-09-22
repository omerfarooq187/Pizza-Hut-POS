package presentation.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.anastaciocintra.escpos.EscPos
import com.github.anastaciocintra.escpos.EscPosConst
import com.github.anastaciocintra.escpos.image.BitImageWrapper
import com.github.anastaciocintra.escpos.image.BitonalOrderedDither
import com.github.anastaciocintra.escpos.image.CoffeeImageImpl
import com.github.anastaciocintra.escpos.image.EscPosImage
import com.github.anastaciocintra.output.PrinterOutputStream
import data.model.*
import data.repository.MenuRepository
import data.repository.OrderRepository
import database.Members
import database.OrderType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import service.InventoryService
import java.awt.color.ColorSpace
import java.awt.image.BufferedImage
import java.awt.image.ColorConvertOp
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import javax.imageio.ImageIO
import javax.print.PrintService
import javax.print.PrintServiceLookup

class OrderViewModel : KoinComponent {
    private val orderRepo: OrderRepository by inject()
    private val menuRepo: MenuRepository by inject()
    private val inventoryService: InventoryService by inject()
    private val coroutineScope = CoroutineScope(Dispatchers.Main.immediate)

    data class OrderState(
        val searchQuery: String = "",
        val memberCode: String = "",
        val isMember: Boolean = false,
        val memberId: Int? = null,
        val orderType: OrderType = OrderType.DELIVERY,
        val items: Map<String, OrderItem> = emptyMap(), // Key: "itemId-variantId"
        val menuItems: List<MenuItemWithVariants> = emptyList(),
        val filteredItems: List<MenuItemWithVariants> = emptyList(),
        val total: Double = 0.0,
        val discount: Double = 0.0,
        val serviceCharges: Int = 0,
        val deliveryRider: String? = null,
        val deliveryAddress: String? = null,
        val phone: String = "",
        val createdAt: Date = Date(),
        val editingOrderId: Int? = null
    )

    private val _state = mutableStateOf(OrderState())
    val state: State<OrderState> = _state

    var alerts by mutableStateOf<List<String>>(emptyList())


    init {
        loadMenuItems()
    }

    private fun loadMenuItems() {
        coroutineScope.launch {
            _state.value = _state.value.copy(
                menuItems = menuRepo.getAllMenuItemsWithVariants(),
                filteredItems = menuRepo.getAllMenuItemsWithVariants()
            )
        }
    }

    fun validateMember(code: String) {
        _state.value = _state.value.copy(memberCode = code)

        transaction {
            val memberRow = Members.select { Members.code eq code.toInt() }.firstOrNull()
            if (memberRow != null) {
                _state.value = _state.value.copy(
                    isMember = true,
                    memberId = memberRow[Members.id].value
                )
            } else {
                _state.value = _state.value.copy(
                    isMember = false,
                    memberId = null
                )
            }
        }

        coroutineScope.launch {
            recalculatePrices()
        }
    }


    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        filterItems()
    }

    private fun filterItems() {
        val query = _state.value.searchQuery.lowercase()
        _state.value = _state.value.copy(
            filteredItems = _state.value.menuItems.filter {
                it.item.name.lowercase().contains(query) ||
                        it.item.description?.lowercase()?.contains(query) == true
            }
        )
    }

    fun addItem(menuItemsWithVariants: MenuItemWithVariants, variant: ItemVariant) {
        val item = menuItemsWithVariants.item
        val key = "${item.id}-${variant.id}"
        val existing = _state.value.items[key]
        val (price, discount) = calculatePriceAndDiscount(item, variant)

        val newItem = existing?.copy(
            quantity = existing.quantity + 1,
            price = price,
            discountApplied = discount
        ) ?: OrderItem(
            itemId = item.id,
            variantId = variant.id,
            itemName = item.name,
            variantSize = variant.size,
            quantity = 1,
            price = price,
            memberPriceApplied = _state.value.isMember,
            discountApplied = discount
        )

        _state.value = _state.value.copy(
            items = _state.value.items + (key to newItem)
        )
        calculateTotals()
    }

    private fun calculatePriceAndDiscount(item: MenuItem, variant: ItemVariant): Pair<Double, Double> {
        return when {
            _state.value.isMember && variant.memberPrice != null ->
                Pair(variant.memberPrice, variant.price - variant.memberPrice)

            _state.value.isMember && item.discountType != null -> {
                val discount = when(item.discountType) {
                    DiscountType.PERCENTAGE -> variant.price * (item.discountValue ?: 0.0) / 100
                    DiscountType.FIXED -> item.discountValue ?: 0.0
                }
                Pair(variant.price - discount, discount)
            }

            else -> Pair(variant.price, 0.0)
        }
    }

    // Modify calculateTotals to include service charges
    private fun calculateTotals() {
        val subtotal = _state.value.items.values.sumOf { it.price * it.quantity } // already discounted
        val totalDiscount = _state.value.items.values.sumOf { it.discountApplied * it.quantity }

        _state.value = _state.value.copy(
            total = subtotal + _state.value.serviceCharges,
            discount = totalDiscount
        )
    }


    fun finalizeOrder() {
        coroutineScope.launch {
            try {
                // Validate inventory first
                val inventoryErrors = inventoryService.validateOrderInventory(
                    _state.value.items.values.map {
                        OrderItem(
                            itemId = it.itemId,
                            variantId = it.variantId,
                            quantity = it.quantity,
                            itemName = it.itemName,
                            variantSize = it.variantSize,
                            price = it.price
                        )
                    }
                )

                if (inventoryErrors.isNotEmpty()) {
                    alerts = inventoryErrors
//                    return@launch
                }

                // Create order entity with status
                val isEditing = _state.value.editingOrderId != null
                val order = if (isEditing) {
                    Order(
                        id = _state.value.editingOrderId ?: 0,
                        totalAmount = _state.value.total,
                        memberId = _state.value.memberId,
                        isMember = _state.value.isMember,
                        items = _state.value.items.values.toList(),
                        orderType = _state.value.orderType,
                        servicesCharges = _state.value.serviceCharges,
                        deliveryRider = if (_state.value.orderType == OrderType.DELIVERY) _state.value.deliveryRider else null,
                        deliveryAddress = if (_state.value.orderType == OrderType.DELIVERY) _state.value.deliveryAddress else null,
                        phone =  if (_state.value.phone != "") _state.value.phone else "",
                        status = OrderStatus.PENDING,
                        createdAt = DateTime.now(),
                        updatedAt = DateTime.now()
                    )
                } else {
                    Order(
                        id = _state.value.editingOrderId ?: 0,
                        totalAmount = _state.value.total,
                        memberId = _state.value.memberId,
                        isMember = _state.value.isMember,
                        items = _state.value.items.values.toList(),
                        orderType = _state.value.orderType,
                        servicesCharges = _state.value.serviceCharges,
                        deliveryRider = if (_state.value.orderType == OrderType.DELIVERY) _state.value.deliveryRider else null,
                        deliveryAddress = if (_state.value.orderType == OrderType.DELIVERY) _state.value.deliveryAddress else null,
                        phone = if (_state.value.phone != "") _state.value.phone else "",
                        status = OrderStatus.PENDING,
                        createdAt = DateTime.now(),
                        updatedAt = DateTime.now()
                    )
                }

                // Save order
                if (isEditing) {
                    orderRepo.updateOrder(order)
                    alerts += "Order updated successfully!"
                    printReceipt(true)
                } else {
                    val newId = orderRepo.createOrder(order)
                    _state.value = _state.value.copy(editingOrderId = newId)
                    alerts += "Order created successfully!"
                    printReceipt(false)
                }

                // Process inventory after successful save
                inventoryService.processOrderInventory(
                    orderId = order.id,
                    isUpdate = isEditing,
                    orderItems = order.items
                )

                // Post-processing
                inventoryService.checkLowStock().forEach {
                    alerts += "Low stock: ${it.name} (${it.currentStock} ${it.unit})"
                }

                clearOrder() // Resets state including editingOrderId

            } catch (e: Exception) {
                val errorMessage = "Order failed: ${e.message ?: "Unknown error"}"
                alerts += errorMessage
                e.printStackTrace()
            }
        }
    }

    fun printReceipt(isOldOrder: Boolean) {
        val printerName = getDefaultPrinter()?.name
        if (printerName != null) {
            printReceiptWithLogo(printerName, state.value.editingOrderId.toString(),buildReceiptContent(), isOldOrder)
        }
    }

    fun updateOrderType(type: OrderType) {
        _state.value = _state.value.copy(orderType = type)
    }

    fun updateServiceCharges(serviceCharges: Int) {
        _state.value = _state.value.copy(serviceCharges = serviceCharges)
        calculateTotals()
    }

    fun clearOrder() {
        _state.value = OrderState() // Reset all state values
        loadMenuItems()
//        alerts = emptyList()
    }
    private suspend fun recalculatePrices() {
        _state.value.items.forEach { (key, item) ->
            menuRepo.getItemById(item.itemId).let { menuItem ->
                menuItem.variants.find { it.id == item.variantId }?.let { variant ->
                    val (newPrice, newDiscount) = calculatePriceAndDiscount(menuItem, variant)
                    _state.value = _state.value.copy(
                        items = _state.value.items + (key to item.copy(
                            price = newPrice,
                            discountApplied = newDiscount,
                            memberPriceApplied = _state.value.isMember
                        ))
                    )
                }
            }
        }
        calculateTotals()
    }

    // Add quantity tracking
    fun getQuantity(itemId: Int, variantId: Int): Int {
        return _state.value.items["${itemId}-${variantId}"]?.quantity ?: 0
    }


    private fun buildReceiptContent(): ByteArray {
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val lineWidth = 48
        val charset = Charset.forName("CP437")

        // ESC/POS commands
        val initialize = byteArrayOf(0x1B, 0x40)
        val boldOn = byteArrayOf(0x1B, 0x45, 0x01)
        val boldOff = byteArrayOf(0x1B, 0x45, 0x00)
        val doubleHeightOn = byteArrayOf(0x1B, 0x21, 0x10) // Height only
        val normalText = byteArrayOf(0x1B, 0x21, 0x00)
        val centerAlign = byteArrayOf(0x1B, 0x61, 0x01)
        val leftAlign = byteArrayOf(0x1B, 0x61, 0x00)
        val cutPaper = byteArrayOf(0x1D, 0x56, 0x41, 0x10)

        // Column widths
        val colItemWidth = 30
        val colQtyWidth = 4
        val colPriceWidth = 12

        return ByteArrayOutputStream().apply {
            write(initialize)
            // Header with double height
            write(centerAlign)
            write(doubleHeightOn)
            write(boldOn)
            write("KITCHEN - ${state.value.orderType}\n".toByteArray(charset))
            write(normalText)
            write(boldOff)
            write("\n".toByteArray(charset)) // Space below header

            // Order info
            write(leftAlign)
            write("Date: ${sdfDate.format(state.value.createdAt)}\n".toByteArray(charset))
            write("Time: ${sdfTime.format(state.value.createdAt)}\n".toByteArray(charset))
//      write("Receipt ID: ${state.value.editingOrderId}\n".toByteArray(charset))
            write("${"-".repeat(lineWidth)}\n".toByteArray(charset))

            // Items table header
            write(boldOn)
            val headerFormat = "%-${colItemWidth}s %-${colQtyWidth}s %${colPriceWidth}s\n"
            write(headerFormat.format("ITEM", "QTY", "PRICE").toByteArray(charset))
            write("${"-".repeat(lineWidth)}\n".toByteArray(charset))
            write(boldOff)

            // Items - include size in item name
            state.value.items.forEach { item ->
                val name = "${item.value.itemName} (${item.value.variantSize})"
                    .uppercase()
                    .take(colItemWidth)

                val qty = "x${item.value.quantity}".take(colQtyWidth)
                val price = String.format("%.2f", item.value.price * item.value.quantity) // total price for that line

                val lineFormat = "%-${colItemWidth}s %-${colQtyWidth}s %${colPriceWidth}s\n"
                val line = lineFormat.format(name, qty, price)
                write(line.toByteArray(charset))
            }

            // Total
            write("${"-".repeat(lineWidth)}\n".toByteArray(charset))
            write(boldOn)

            val totalQuantity = state.value.items.values.sumOf { it.quantity }
            val totalAmount = state.value.items.values.sumOf { it.price * it.quantity }

            val totalLine = String.format(
                "%-${colItemWidth}s %-${colQtyWidth}s %${colPriceWidth}.2f\n",
                "TOTAL:",
                "x$totalQuantity",
                totalAmount
            )
            write(totalLine.toByteArray(charset))

            write(boldOff)
            write("${"=".repeat(lineWidth)}\n".toByteArray(charset))

            // Footer
            write(centerAlign)
            write(boldOn)
            write("THANK YOU!\n\n\n".toByteArray(charset)) // 3 newlines for paper tear
            write(boldOff)
            write(cutPaper)
        }.toByteArray()
    }


    fun printReceiptWithLogo(printerName: String, receiptId: String, receiptData: ByteArray, isOldOrder: Boolean) {
        var escpos: EscPos? = null
        var outputStream: PrinterOutputStream? = null

        try {
            val printServices = PrinterOutputStream.getListPrintServicesNames()
            if (!printServices.contains(printerName)) {
                throw IllegalArgumentException("Printer '$printerName' not found. Available printers: ${printServices.joinToString()}")
            }

            val printService = PrinterOutputStream.getPrintServiceByName(printerName)
            outputStream = PrinterOutputStream(printService)
            escpos = EscPos(outputStream)

            val charset = Charset.forName("CP437")
            val initialize = byteArrayOf(0x1B, 0x40)
            val boldOn = byteArrayOf(0x1B, 0x45, 0x01)
            val boldOff = byteArrayOf(0x1B, 0x45, 0x00)
            val doubleSize = byteArrayOf(0x1B, 0x21, 0x30)
            val normalSize = byteArrayOf(0x1B, 0x21, 0x00)

            // --- Start printing ---
            outputStream.write(initialize)
            outputStream.write(doubleSize)
            outputStream.write(boldOn)

            // Always print receipt ID
            outputStream.write("Receipt ID: $receiptId\n".toByteArray(charset))

            // If editing -> mark as OLD ORDER
            if (isOldOrder) {
                outputStream.write("OLD ORDER\n".toByteArray(charset))
                println("Order is old")
            }

            outputStream.write(boldOff)
            outputStream.write(normalSize)
            outputStream.write("\n".toByteArray(charset))

            // Print logo
            val imageStream = javaClass.getResourceAsStream("/logo.jpg").use { stream ->
                val originalImage = ImageIO.read(stream)
                val targetWidth = 300
                val targetHeight = (originalImage.height * targetWidth) / originalImage.width

                val resizedImage = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB).apply {
                    createGraphics().run {
                        drawImage(originalImage, 0, 0, targetWidth, targetHeight, null)
                        dispose()
                    }
                }

                EscPosImage(
                    CoffeeImageImpl(convertToHighContrast(resizedImage)),
                    BitonalOrderedDither()
                )
            }

            escpos.write(
                BitImageWrapper().setJustification(EscPosConst.Justification.Center),
                imageStream
            )

            escpos.feed(2)
            outputStream.write(receiptData)
            outputStream.flush()

            escpos.cut(EscPos.CutMode.FULL)

        } catch (e: Exception) {
            System.err.println("Error printing: ${e.message}")
            e.printStackTrace()
        } finally {
            escpos?.close()
            outputStream?.close()
        }
    }



    private fun convertToHighContrast(image: BufferedImage): BufferedImage {
        val converter = ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null)
        return converter.filter(image, null)
    }
    fun updateDeliveryDetails(rider: String, address: String, phone: String) {
        _state.value = _state.value.copy(
            deliveryRider = rider,
            deliveryAddress = address,
            phone = phone
        )
    }

    // In OrderViewModel
    fun handleEditOrder(orderId: Int?) {
        coroutineScope.launch {
            try {
                val order = orderRepo.getOrderById(orderId!!)
                _state.value = _state.value.copy(
                    items = order.items.associateBy { "${it.itemId}-${it.variantId}" },
                    orderType = order.orderType,
                    deliveryRider = order.deliveryRider,
                    deliveryAddress = order.deliveryAddress,
                    serviceCharges = order.servicesCharges,
                    memberCode = order.memberId?.toString() ?: "",
                    isMember = order.isMember,
                    editingOrderId = orderId
                )
                recalculatePrices()
                calculateTotals()
            } catch (e: Exception) {
                alerts += ("Failed to load order: ${e.message}")
            }
        }
    }

    fun removeItem(orderItem: OrderItem) {
        val key = "${orderItem.itemId}-${orderItem.variantId}"
        _state.value = _state.value.copy(
            items = _state.value.items - key
        )
        calculateTotals()
    }

    private fun getDefaultPrinter(): PrintService? {
        return PrintServiceLookup.lookupDefaultPrintService()
    }
}
