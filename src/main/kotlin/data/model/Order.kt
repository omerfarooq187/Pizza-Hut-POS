package data.model

import database.OrderType
import org.joda.time.DateTime

// data/model/Order.kt
data class Order(
    val id: Int = 0,
    val customerName: String = "",
    val phone: String = "",
    val email: String = "",
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val memberId: Int? = null,
    val isMember: Boolean = false,
    val orderType: OrderType = OrderType.DELIVERY,
    val servicesCharges: Int,
    val deliveryRider: String? = null,
    val deliveryAddress: String? = null,
    val createdAt: DateTime = DateTime.now(),
    val status: OrderStatus = OrderStatus.PENDING,
    val updatedAt: DateTime = DateTime.now()
)

data class OrderItem(
    val id: Int = 0,
    val itemId: Int,
    val variantId: Int,  // Add this field
    val itemName: String,
    val variantSize: String,
    val quantity: Int,
    val price: Double,
    val memberPriceApplied: Boolean = false,
    val discountApplied: Double = 0.0
)

enum class OrderStatus {
    PENDING, COMPLETED, CANCELLED
}