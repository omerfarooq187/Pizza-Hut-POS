package presentation.viewmodel

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
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import data.model.Order
import data.model.OrderStatus
import data.repository.OrderRepository
import data.repository.Quadruple
import database.Members
import database.OrderType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.awt.Color
import java.awt.color.ColorSpace
import java.awt.image.BufferedImage
import java.awt.image.ColorConvertOp
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.contains

class ReportViewModel : KoinComponent {
    private val orderRepo: OrderRepository by inject()
    var orders by mutableStateOf<List<Order>>(emptyList())
    var isMember by mutableStateOf<Boolean>(false)
    var memberCode by mutableStateOf<String>("")
    private val coroutineScope = CoroutineScope(Dispatchers.Main.immediate)
    var exportMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadOrders()
    }

    var memberFilterError by mutableStateOf<String?>(null)
    private var foundMemberId by mutableStateOf<Int?>(null)

    var selectedRange by mutableStateOf("Today")
    var selectedMemberFilter by mutableStateOf("ALL")
    var memberCodeFilter by mutableStateOf("")
    var selectedStatus by mutableStateOf("ALL")

    // In ReportViewModel.kt
    fun filterOrders() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // 1. Fetch orders based on date range
                val dateFiltered = when (selectedRange) {
                    "Today" -> orderRepo.getDailyOrders()
                    "This Week" -> orderRepo.getWeeklyOrders()
                    "This Month" -> orderRepo.getMonthlyOrders()
                    else -> orderRepo.getOrdersByDateRange(
                        start = DateTime.now().withTimeAtStartOfDay(),
                        end = DateTime.now()
                    )
                }

                // 2. Apply status filter
                val statusFiltered = if (selectedStatus != "ALL") {
                    dateFiltered.filter { it.status.name == selectedStatus }
                } else {
                    dateFiltered
                }

                // 3. Apply member type filter (fix comparison values)
                val typeFiltered = when (selectedMemberFilter) {
                    "Members" -> statusFiltered.filter { it.isMember }
                    "Non-Member" -> statusFiltered.filter { !it.isMember }
                    else -> statusFiltered
                }

                // 4. Apply member code filter (use correct variable)
                val codeFiltered = if (memberCodeFilter.isNotEmpty()) {
                    val memberId = validateAndGetMemberId(memberCodeFilter)
                    typeFiltered.filter { it.memberId == memberId }
                } else {
                    typeFiltered
                }

                // Update UI state
                coroutineScope.launch {
                    orders = codeFiltered
                    memberFilterError = if (memberCodeFilter.isNotEmpty() && foundMemberId == null) {
                        "Invalid member code"
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle error
            }
        }
    }

    private fun validateAndGetMemberId(code: String): Int? {
        return try {
            transaction {
                Members.select { Members.code eq code.toInt() }
                    .firstOrNull()
                    ?.get(Members.id)
                    ?.value
                    .also { foundMemberId = it }
            }
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            null
        }
    }

    // Update the validateMember function
    fun validateMember(code: String) {
        memberCodeFilter = code
        foundMemberId = validateAndGetMemberId(code)
        isMember = foundMemberId != null
    }


    private fun loadOrders() {
        coroutineScope.launch {
            orders = orderRepo.getOrdersByDateRange(
                start = DateTime.now().minusMonths(1),
                end = DateTime.now()
            )
        }
    }

    fun getAllOrders(): List<Order> = orders


    fun exportToPdf(orders: List<Order>) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val reportsDir = File(System.getProperty("user.home"), "YourApp/Reports").apply {
                    if (!exists()) mkdirs()
                }

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val pdfFile = File(reportsDir, "report_$timestamp.pdf")

                PdfDocument(PdfWriter(pdfFile)).use { pdfDoc ->
                    val document = Document(pdfDoc)
                    document.add(Paragraph("Sales Report"))

                    // Add table content
                    orders.forEach { order ->
                        document.add(Paragraph("Order ID: ${order.id}"))
                        document.add(Paragraph("Customer: ${order.customerName}"))
                        document.add(Paragraph("Total: Rs.${"%.2f".format(order.totalAmount)}"))
                        document.add(Paragraph("\n"))
                    }

                    document.close()
                }

               coroutineScope.launch {
                    exportMessage = "PDF saved to:\n${pdfFile.absolutePath}"
                }
            } catch (e: Exception) {
                coroutineScope.launch {
                    exportMessage = "PDF export failed: ${e.localizedMessage}"
                }
            }
        }
    }

    fun exportToExcel(orders: List<Order>) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val reportsDir = File(System.getProperty("user.home"), "YourApp/Reports").apply {
                    if (!exists()) mkdirs()
                }

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val excelFile = File(reportsDir, "report_$timestamp.xls")

                HSSFWorkbook().use { workbook ->
                    val sheet = workbook.createSheet("Orders Report")

                    // Create header row
                    val headerRow = sheet.createRow(0)
                    headerRow.createCell(0).setCellValue("Order ID")
                    headerRow.createCell(1).setCellValue("Customer Name")
                    headerRow.createCell(2).setCellValue("Total Amount")
                    headerRow.createCell(3).setCellValue("Date")

                    // Populate data
                    orders.forEachIndexed { index, order ->
                        val row = sheet.createRow(index + 1)
                        row.createCell(0).setCellValue(order.id.toString())
                        row.createCell(1).setCellValue(order.customerName)
                        row.createCell(2).setCellValue(order.totalAmount)
                        row.createCell(3).setCellValue(
                            SimpleDateFormat("yyyy-MM-dd HH:mm").format(order.createdAt)
                        )
                    }

                    // Auto-size columns
                    for (i in 0..3) {
                        sheet.autoSizeColumn(i)
                    }

                    FileOutputStream(excelFile).use { fos ->
                        workbook.write(fos)
                    }
                }

                coroutineScope.launch {
                    exportMessage = "Excel saved to:\n${excelFile.absolutePath}"
                }
            } catch (e: Exception) {
                coroutineScope.launch {
                    exportMessage = "Excel export failed: ${e.localizedMessage}"
                }
            }
        }
    }

    fun clearMessage() {
        exportMessage = null
    }


    private fun buildReceiptContent(order: Order): ByteArray {
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm aa", Locale.getDefault())
        val lineWidth = 48
        val charset = Charset.forName("CP437")

        // Column widths (sum = 46, leaves 2 spaces for separation)
        val colItemWidth = 30  // ITEM (now includes size)
        val colQtyWidth = 4    // QTY
        val colPriceWidth = 12  // PRICE (for proper currency alignment)

        // ESC/POS commands
        val initialize = byteArrayOf(0x1B, 0x40)
        val boldOn = byteArrayOf(0x1B, 0x45, 0x01)
        val boldOff = byteArrayOf(0x1B, 0x45, 0x00)
        val doubleHeightOn = byteArrayOf(0x1B, 0x21, 0x10) // Height only
        val normalText = byteArrayOf(0x1B, 0x21, 0x00)
        val centerAlign = byteArrayOf(0x1B, 0x61, 0x01)
        val leftAlign = byteArrayOf(0x1B, 0x61, 0x00)
        val cutPaper = byteArrayOf(0x1D, 0x56, 0x41, 0x10)

        return ByteArrayOutputStream().apply {
            write(initialize)

            // Header with double height
            write(centerAlign)
            write(doubleHeightOn)
            write("MANDRA PIZZA HUT\n".toByteArray(charset))
            write(normalText)
            write("\n".toByteArray(charset)) // Added space below restaurant name

            write(leftAlign)
            write(boldOn)
            write("Main G.T Rd Near Leather Point\n".toByteArray(charset))
            write("Tel: 051-3591155  WhatsApp: 0309-5107040\n".toByteArray(charset))
            write(boldOff)
            write("${"=".repeat(lineWidth)}\n".toByteArray(charset))

            // Order info
            write(boldOn)
            write("ORDER TIME: ${sdf.format(order.createdAt.toDate())}\n".toByteArray(charset))
            write("ORDER TYPE: ${order.orderType}\n\n".toByteArray(charset))
            write(boldOff)

            if (order.orderType == OrderType.DELIVERY) {
                order.deliveryRider?.let { write("RIDER: ${it.uppercase()}\n".toByteArray(charset)) }
                order.deliveryAddress?.let { write("CUSTOMER ADDRESS: ${it.uppercase()}\n\n".toByteArray(charset)) }
                write("CUSTOMER PHONE: ${order.phone}\n\n".toByteArray(charset))
            }

            // Items table header
            write(boldOn)
            val headerFormat = "%-${colItemWidth}s %${colQtyWidth}s %${colPriceWidth}s\n"
            write(headerFormat.format("ITEM", "QTY", "PRICE").toByteArray(charset))
            write("${"-".repeat(lineWidth)}\n".toByteArray(charset))
            write(boldOff)

            // Items - include size in item name
            order.items.forEach { item ->
                val name = "${item.itemName.uppercase()} (${item.variantSize.uppercase()})".take(colItemWidth)
                val qty = "x${item.quantity}".take(colQtyWidth)

                // Format price with consistent RS. prefix and decimal alignment
                val price = "%,.2f".format(item.price * item.quantity)
                val formattedPrice = "RS.$price"

                val lineFormat = "%-${colItemWidth}s %${colQtyWidth}s %${colPriceWidth}s\n"
                val line = lineFormat.format(name, qty, formattedPrice)
                write(line.toByteArray(charset))
            }

            // Totals with proper price alignment
            write("${"-".repeat(lineWidth)}\n".toByteArray(charset))
            write(boldOn)

            // Format service charges
            val servicePrice = "%,.2f".format(order.servicesCharges.toDouble())
            val serviceLine = String.format(
                "%-${colItemWidth + colQtyWidth + 1}s RS.%${colPriceWidth - 3}s\n",
                "SERVICE CHARGES:",
                servicePrice
            )
            write(serviceLine.toByteArray(charset))

            // Format total amount
            val totalPrice = "%,.2f".format(order.totalAmount)
            val totalLine = String.format(
                "%-${colItemWidth + colQtyWidth + 1}s RS.%${colPriceWidth - 3}s\n",
                "TOTAL AMOUNT:",
                totalPrice
            )
            write(totalLine.toByteArray(charset))

            write(boldOff)
            write("${"=".repeat(lineWidth)}\n".toByteArray(charset))

            // Footer
            write(centerAlign)
            write(boldOn)
            write("THANK YOU FOR YOUR ORDER!\n".toByteArray(charset))
            write("ENJOY YOUR MEAL\n\n\n".toByteArray(charset)) // 3 newlines for paper tear
            write(boldOff)
            write(cutPaper)
        }.toByteArray()
    }

//    private fun formatLine(item: String, qty: String, price: String, itemWidth: Int, qtyWidth: Int, priceWidth: Int): String {
//        val itemCol = if (item.length > itemWidth) item.take(itemWidth - 1) + "…" else item.padEnd(itemWidth)
//        val qtyCol = qty.padStart(qtyWidth)
//        val priceCol = price.padStart(priceWidth)
//        // Add spaces between columns
//        return "$itemCol $qtyCol $priceCol"
//    }

    fun printReceiptForOrder(order: Order) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val receiptContent = buildReceiptContent(order)
                printReceiptWithLogo("Black Copper BC-85AC", receiptContent)
                println(receiptContent)
                exportMessage = "Receipt printed successfully"
            } catch (e: Exception) {
                exportMessage = "Print failed: ${e.message}"
            }
        }
    }


    fun printReceiptWithLogo(printerName: String, receiptData: ByteArray) {
        var escpos: EscPos? = null
        var outputStream: PrinterOutputStream? = null

        try {
            // 1. Validate printer exists
            val printServices = PrinterOutputStream.getListPrintServicesNames()
            if (!printServices.contains(printerName)) {
                throw IllegalArgumentException("Printer '$printerName' not found. Available printers: ${printServices.joinToString()}")
            }

            // 2. Initialize printer connection
            val printService = PrinterOutputStream.getPrintServiceByName(printerName)
            outputStream = PrinterOutputStream(printService)
            escpos = EscPos(outputStream)

            // 3. Print logo
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
                BitImageWrapper()
                    .setJustification(EscPosConst.Justification.Center),
                imageStream
            )

            // 4. Print receipt content from byte array
            escpos.feed(2) // Add space after logo
            outputStream.write(receiptData)
            outputStream.flush()

            // 5. Add paper cut (if not already in receiptData)
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


    // Change to Triple<code, name, count>
    var topMembers by mutableStateOf<List<Quadruple<Int, String, String, Long>>>(emptyList())
    // Add this function
    fun loadMemberStatistics() {
        coroutineScope.launch(Dispatchers.IO) {
            topMembers = orderRepo.getTopMembersByOrders()
        }
    }

    // Update refresh function
    fun refreshOrders() {
        filterOrders()
        loadMemberStatistics()
        memberFilterError = null
        foundMemberId = null
    }

    // ReportViewModel.kt
    fun updateOrderStatus(orderId: Int, newStatus: OrderStatus) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                orderRepo.updateOrderStatus(orderId, newStatus)
                refreshOrders()
                loadOrders()
            } catch (e: Exception) {
                exportMessage = "Failed to update status: ${e.message}"
            }
        }
    }
}