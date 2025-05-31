package data.model


// data/model/MenuItem.kt
data class MenuItem(
    val id: Int = 0,
    val categoryId: Int,
    val name: String,
    val description: String? = null,
    val variants: List<ItemVariant> = emptyList(),
    val isActive: Boolean = true,
    val discountType: DiscountType? = null,
    val discountValue: Double? = null
)

// data/model/ItemVariant.kt
data class ItemVariant(
    val id: Int = 0,
    val itemId: Int = 0,          // From ItemVariants.itemId
    val size: String,         // From ItemVariants.size
    val price: Double,        // From ItemVariants.price
    val memberPrice: Double? = null, // From ItemVariants.memberPrice
    val recipes: List<Recipe>? = emptyList(),
    val itemName: String = ""    // From joined MenuItems.name
)

data class MenuItemWithVariants(
    val item: MenuItem,
    val variants: List<ItemVariant>,
    val categoryName: String
)

enum class DiscountType {
    PERCENTAGE,
    FIXED
}