package database

import data.model.DiscountType
import data.model.OrderStatus
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.jodatime.datetime
import org.joda.time.DateTime


object Categories : Table("categories") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50).uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}

object MenuItems: IntIdTable("menu_items") {
    val categoryId = integer("category_id").references(Categories.id)
    val name = varchar("name", 100)
    val description = text("description").nullable()
    val isActive = bool("is_active").default(true)
    val discountType = enumerationByName("discount_type", 20, DiscountType::class).nullable()
    val discountValue = double("discount_value").nullable()
}

object ItemVariants: IntIdTable("item_variants") {
    val itemId = integer("item_id").references(MenuItems.id)
    val size = varchar("size", 20)
    val price = double("price")
    val memberPrice = double("member_price").nullable()
}

// database/Orders.kt
object Orders : IntIdTable("orders") {
    val customerName = varchar("customer_name", 100).default("N/A")
    val phone = varchar("phone", 20).default("N/A")
    val email = varchar("email", 50).default("N/A")
    val totalAmount = double("total_amount").default(0.0)
    val memberId = reference("member_id", Members).nullable()
    val isMember = bool("is_member").default(false)
    val orderType = enumerationByName("order_type", 20, OrderType::class).default(OrderType.DELIVERY)
    val servicesCharges = integer("services_charges").default(0)
    val deliveryRider = varchar("delivery_rider", 50).nullable().default(null)
    val deliveryAddress = varchar("delivery_address", 200).nullable().default(null)
    val createdAt = datetime("created_at").default(DateTime.now())
    val status = enumerationByName("status", 20, OrderStatus::class).default(OrderStatus.PENDING)
    val updatedAt = datetime("updated_at").default(DateTime.now())
}

enum class OrderType {
    DINEIN, TAKEAWAY, DELIVERY
}

object OrderItems : IntIdTable("order_items") {
    val orderId = integer("order_id").references(Orders.id)
    val itemId = integer("item_id").references(MenuItems.id)
    val variantId = integer("variant_id").references(ItemVariants.id)
    val variantSize = varchar("variant_size", 20)
    val quantity = integer("quantity")
    val price = double("price")  // Price at time of ordering
    val memberPriceApplied = bool("member_price_applied").default(false)
    val discountApplied = double("discount_applied").default(0.0)
}

object Members : IntIdTable("members") {
//    val orders = OpEntityRelation<Orders?>(Orders.memberId)
    val phone = varchar("phone", 20).uniqueIndex()
    val name = varchar("name", 50)
    val code = integer("code").uniqueIndex()
    val address = varchar("address", length = 200)
}

// database/RawItems.kt
object RawItems : IntIdTable("raw_items") {
    val name = varchar("name", 100).uniqueIndex()
    val description = text("description").nullable()
    val unit = varchar("unit", 20)  // kg, grams, liters, pieces, etc.
    val currentStock = double("current_stock").default(0.0)
    val alertThreshold = double("alert_threshold").nullable()
    val supplier = varchar("supplier", 100).nullable()
}

// database/Recipes.kt
object Recipes : Table("recipes") {
    val variantId = integer("variant_id").references(ItemVariants.id)
    val rawItemId = integer("raw_item_id").references(RawItems.id)
    val quantityNeeded = double("quantity_needed")

    override val primaryKey = PrimaryKey(variantId, rawItemId)
}

// database/InventoryTransactions.kt
object InventoryTransactions : IntIdTable("inventory_transactions") {
    val rawItemId = integer("raw_item_id").references(RawItems.id)
    val amount = double("amount")  // Positive for additions, negative for deductions
    val transactionType = enumerationByName("transaction_type", 20, TransactionType::class)
    val date = datetime("date").default(DateTime.now())
    val orderId = integer("order_id").references(Orders.id).nullable()
    val notes = text("notes").nullable()
}

enum class TransactionType {
    PURCHASE, USAGE, ADJUSTMENT, WASTAGE, RETURN
}

// database/DailyInventory.kt
object DailyInventory : Table("daily_inventory") {
    val date = datetime("date")
    val rawItemId = integer("raw_item_id").references(RawItems.id)
    val startingAmount = double("starting_amount")
    val endingAmount = double("ending_amount").nullable()
    val physicalCount = double("physical_count").nullable()
    val notes = text("notes").nullable()

    override val primaryKey = PrimaryKey(date, rawItemId)
}