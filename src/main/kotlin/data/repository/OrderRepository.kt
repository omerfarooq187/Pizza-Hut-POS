package data.repository

import data.model.Order
import data.model.OrderItem
import data.model.OrderStatus
import database.Members
import database.MenuItems
import database.OrderItems
import database.Orders
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

// data/repository/OrderRepository.kt
interface OrderRepository {
    // Order Operations
    suspend fun createOrder(order: Order): Int
//    suspend fun updateOrderStatus(orderId: Int)
    suspend fun getOrdersByDateRange(start: DateTime, end: DateTime): List<Order>

    // Order Item Operations
    suspend fun addItemToOrder(orderId: Int, item: OrderItem)
    suspend fun removeItemFromOrder(orderItemId: Int)

    // Reports
    // Add to OrderRepository interface
//    suspend fun getOrdersByStatus(status: OrderStatus): List<Order>

    suspend fun getDailyOrders(): List<Order>
    suspend fun getWeeklyOrders(): List<Order>
    suspend fun getMonthlyOrders(): List<Order>
    suspend fun searchSoldItems(itemName: String): List<OrderItem>

    suspend fun isPhoneNumberRegistered(phone: String): Boolean


    suspend fun getTodayTotalSales(): Double
    suspend fun getTodayTotalOrdersCount(): Long

    suspend fun updateOrder(order: Order)
    suspend fun getOrderById(orderId: Int): Order
    suspend fun getOrdersByStatus(status: OrderStatus): List<Order>
    suspend fun updateOrderStatus(orderId: Int, status: OrderStatus)
    suspend fun getTopMembersByOrders(limit: Int = 5): List<Quadruple<Int, String, String, Long>>
}

class OrderRepositoryImpl: OrderRepository {
    // OrderRepositoryImpl.kt
    override suspend fun createOrder(order: Order): Int = transaction {
        // Insert order
        val orderId = Orders.insert {
            it[createdAt] = order.createdAt
            it[customerName] = order.customerName
            it[phone] = order.phone
            it[email] = order.email
            it[totalAmount] = order.totalAmount
            it[isMember] = order.isMember
            it[memberId] = order.memberId
            it[orderType] = order.orderType
            it[servicesCharges] = order.servicesCharges
            it[deliveryRider] = order.deliveryRider
            it[deliveryAddress] = order.deliveryAddress
        } get Orders.id

        // Insert order items
        order.items.forEach { item ->
            OrderItems.insert {
                it[OrderItems.orderId] = orderId.value
                it[itemId] = item.itemId
                it[variantId] = item.variantId
                it[variantSize] = item.variantSize
                it[quantity] = item.quantity
                it[price] = item.price
                it[memberPriceApplied] = item.memberPriceApplied
            }
        }

        orderId.value
    }


    override suspend fun getOrdersByDateRange(start: DateTime, end: DateTime): List<Order> =
        transaction {
            Orders.select { Orders.createdAt.between(start, end) }
                .map { rowToOrder(it) }
        }

    override suspend fun addItemToOrder(orderId: Int, item: OrderItem) {
        transaction {
            OrderItems.insert {
                it[this.orderId] = orderId
                it[this.itemId] = item.itemId
                it[variantSize] = item.variantSize
                it[quantity] = item.quantity
                it[price] = item.price
                it[memberPriceApplied] = item.memberPriceApplied
                it[discountApplied] = item.discountApplied
            }
        }
    }

    override suspend fun removeItemFromOrder(orderItemId: Int) {
        transaction {
            OrderItems.deleteWhere { OrderItems.id eq orderItemId }
        }
    }


    override suspend fun getDailyOrders(): List<Order> {
        val todayStart = DateTime.now().withTimeAtStartOfDay()
        val todayEnd = todayStart.plusDays(1)
        return getOrdersByDateRange(todayStart, todayEnd)
    }

    override suspend fun getWeeklyOrders(): List<Order> {
        val end = DateTime.now()
        val start = end.minusDays(7)

        return getOrdersByDateRange(start, end)
    }

    override suspend fun getMonthlyOrders(): List<Order> {
        val end = DateTime.now()
        val start = end.minusDays(30)
        return getOrdersByDateRange(start, end)
    }

    override suspend fun searchSoldItems(itemName: String): List<OrderItem> {
        return transaction {
            (OrderItems innerJoin MenuItems)
                .select { MenuItems.name.lowerCase() like "%${itemName.lowercase()}%" }
                .map { rowToOrderItem(it) }
        }
    }

    override suspend fun isPhoneNumberRegistered(phone: String): Boolean {
        return transaction {
            Members.select { Members.phone eq phone }.count() > 0
        }
    }

    // Existing helper methods
    private fun rowToOrderItem(row: ResultRow): OrderItem {
        return OrderItem(
            id = row[OrderItems.id].value,
            itemId = row[OrderItems.itemId],
            variantSize = row[OrderItems.variantSize],
            variantId = row[OrderItems.variantId],
            quantity = row[OrderItems.quantity],
            price = row[OrderItems.price],
            memberPriceApplied = row[OrderItems.memberPriceApplied],
            discountApplied = row[OrderItems.discountApplied],
            itemName = row[MenuItems.name]
        )
    }



    override suspend fun getTodayTotalSales(): Double = transaction {
        val todayStart = DateTime.now().withTimeAtStartOfDay()
        val todayEnd = todayStart.plusDays(1)

        Orders
            .slice(Orders.totalAmount.sum())
            .select { Orders.createdAt.between(todayStart, todayEnd) and (Orders.status neq OrderStatus.CANCELLED) }
            .firstOrNull()?.getOrNull(Orders.totalAmount.sum()) ?: 0.0
    }

    override suspend fun getTodayTotalOrdersCount(): Long = transaction {
        val todayStart = DateTime.now().withTimeAtStartOfDay()
        val todayEnd = todayStart.plusDays(1)

        Orders
            .select { Orders.createdAt.between(todayStart, todayEnd) }
            .count()
    }

    // In OrderRepositoryImpl
    override suspend fun updateOrder(order: Order) = transaction {
        Orders.update({ Orders.id eq order.id }) {
            it[totalAmount] = order.totalAmount
            it[memberId] = order.memberId
            it[isMember] = order.isMember
            it[orderType] = order.orderType
            it[servicesCharges] = order.servicesCharges
            it[deliveryRider] = order.deliveryRider
            it[deliveryAddress] = order.deliveryAddress
            it[status] = order.status
            it[updatedAt] = DateTime.now()

        }

        // Update order items
        OrderItems.deleteWhere { OrderItems.orderId eq order.id }
        order.items.forEach { item ->
            OrderItems.insert {
                it[OrderItems.orderId] = order.id
                it[itemId] = item.itemId
                it[variantId] = item.variantId
                it[variantSize] = item.variantSize
                it[quantity] = item.quantity
                it[price] = item.price
                it[memberPriceApplied] = item.memberPriceApplied
                it[discountApplied] = item.discountApplied
            }
        }
    }

    override suspend fun getOrderById(orderId: Int): Order = transaction {
        Orders.select { Orders.id eq orderId }
            .first()
            .let { rowToOrder(it) }
    }

    override suspend fun getOrdersByStatus(status: OrderStatus): List<Order> = transaction {
        Orders.select { Orders.status eq status }
            .map { rowToOrder(it) }
    }

    override suspend fun updateOrderStatus(orderId: Int, status: OrderStatus) {
        transaction {
            Orders.update({ Orders.id eq orderId }) {
                it[Orders.status] = status
                it[Orders.updatedAt] = DateTime.now()
            }
        }
    }


    // Update rowToOrder to include status
    private fun rowToOrder(row: ResultRow): Order {
        val items = (OrderItems innerJoin MenuItems)
            .select { OrderItems.orderId eq row[Orders.id].value }
            .map { rowToOrderItem(it) }

        return Order(
            id = row[Orders.id].value,
            customerName = row[Orders.customerName],
            phone = row[Orders.phone],
            email = row[Orders.email],
            items = items,
            totalAmount = row[Orders.totalAmount],
            memberId = row[Orders.memberId]?.value,
            isMember = row[Orders.isMember],
            orderType = row[Orders.orderType],
            servicesCharges = row[Orders.servicesCharges],
            deliveryRider = row[Orders.deliveryRider],
            status = row[Orders.status],
            deliveryAddress = row[Orders.deliveryAddress],
            createdAt = row[Orders.createdAt],
            updatedAt = row[Orders.updatedAt]
        )
    }

    // In OrderRepository
    override suspend fun getTopMembersByOrders(limit: Int): List<Quadruple<Int, String, String, Long>> {
        return transaction {
            (Orders innerJoin Members)
                .slice(Members.code, Members.name, Members.phone, Orders.id.count())
                .select { Orders.memberId.isNotNull() }
                .groupBy(Orders.memberId)
                .orderBy(Orders.id.count() to SortOrder.DESC)
                .limit(limit)
                .map {
                    Quadruple(
                        it[Members.code],
                        it[Members.name],
                        it[Members.phone],
                        it[Orders.id.count()]
                    )
                }
        }
    }
}
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

