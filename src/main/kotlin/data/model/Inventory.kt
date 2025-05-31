package data.model

import database.TransactionType
import org.joda.time.DateTime

// data/model/RawItem.kt
data class RawItem(
    val id: Int = 0,
    val name: String,
    val description: String?,
    val unit: String,
    val currentStock: Double,
    val alertThreshold: Double?,
    val supplier: String?
)

// data/model/Recipe.kt
data class Recipe(
    val variantId: Int,
    val rawItemId: Int,
    val quantityNeeded: Double
)

// data/model/InventoryTransaction.kt
data class InventoryTransaction(
    val id: Int,
    val rawItemId: Int,
    val amount: Double,
    val transactionType: TransactionType,
    val date: DateTime,
    val orderId: Int?,
    val notes: String?
)