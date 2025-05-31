package service

import data.model.OrderItem
import data.model.RawItem
import data.repository.OrderRepository
import data.repository.RawItemRepository
import data.repository.RecipeRepository

class InventoryService(
    private val recipeRepo: RecipeRepository,
    private val rawItemRepo: RawItemRepository,
    private val orderRepo: OrderRepository
) {
    // Update InventoryService
    suspend fun processOrderInventory(
        orderId: Int,
        isUpdate: Boolean,
        orderItems: List<OrderItem>
    ): List<String> {
        val warnings = mutableListOf<String>()

        if (isUpdate) {
            handleOrderUpdate(orderId, warnings)
        }

        orderItems.forEach { item ->
            processItemInventory(item, orderId, warnings)
        }

        return warnings
    }

    private suspend fun handleOrderUpdate(orderId: Int, warnings: MutableList<String>) {
        try {
            val originalOrder = orderRepo.getOrderById(orderId)
            reversePreviousInventory(originalOrder.items, orderId, warnings)
        } catch (e: Exception) {
            warnings.add("⚠️ Failed to reverse previous inventory: ${e.message}")
        }
    }

    private suspend fun reversePreviousInventory(
        originalItems: List<OrderItem>,
        orderId: Int,
        warnings: MutableList<String>
    ) {
        originalItems.forEach { item ->
            recipeRepo.getRecipesForVariant(item.variantId).forEach { recipe ->
                try {
                    rawItemRepo.updateStock(
                        rawItemId = recipe.rawItemId,
                        delta = recipe.quantityNeeded * item.quantity,
                        reason = "Order #$orderId reversal",
                        orderId = orderId
                    )
                } catch (e: Exception) {
                    warnings.add("⚠️ Failed to reverse ${recipe.rawItemId} stock: ${e.message}")
                }
            }
        }
    }

    private suspend fun processItemInventory(
        item: OrderItem,
        orderId: Int,
        warnings: MutableList<String>
    ) {
        try {
            val recipes = recipeRepo.getRecipesForVariant(item.variantId)

            if (recipes.isEmpty()) {
                warnings.add("⚠️ No recipe found for ${item.itemName} - inventory not updated")
                return
            }

            recipes.forEach { recipe ->
                val required = recipe.quantityNeeded * item.quantity
                try {
                    rawItemRepo.updateStock(
                        rawItemId = recipe.rawItemId,
                        delta = -required,
                        reason = "Order #$orderId - ${item.itemName}",
                        orderId = orderId
                    )
                } catch (e: Exception) {
                    warnings.add("⛔ Failed to update ${recipe.rawItemId}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            warnings.add("⚠️ Error processing ${item.itemName}: ${e.message}")
        }
    }

    suspend fun validateOrderInventory(orderItems: List<OrderItem>): List<String> {
        val warnings = mutableListOf<String>()

        orderItems.forEach { item ->
            try {
                val recipes = recipeRepo.getRecipesForVariant(item.variantId)

                if (recipes.isEmpty()) {
                    warnings += "⚠️ No recipe found for ${item.itemName} (${item.variantSize}) - inventory not updated"
                } else {
                    recipes.forEach { recipe ->
                        val rawItem = rawItemRepo.getRawItem(recipe.rawItemId)

                        when {
                            rawItem == null -> {
                                warnings += "⚠️ Missing ingredient: Recipe requires " +
                                        "ID ${recipe.rawItemId} for ${item.itemName}"
                            }

                            rawItem.currentStock < (recipe.quantityNeeded * item.quantity) -> {
                                val needed = recipe.quantityNeeded * item.quantity
                                warnings += "⚠️ Low stock: ${rawItem.name} " +
                                        "(Need $needed ${rawItem.unit}, " +
                                        "Have ${rawItem.currentStock})"
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                warnings += "⚠️ Validation error for ${item.itemName}: ${e.message}"
            }
        }

        return warnings
    }

    suspend fun checkLowStock(): List<RawItem> {
        return rawItemRepo.getAllRawItemsBelowThreshold()
    }

}