package data.repository

import data.model.ItemVariant
import data.model.MenuItem
import data.model.MenuItemWithVariants
import database.Categories
import database.ItemVariants
import database.MenuItems
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

interface MenuRepository {
    suspend fun getItemsByCategory(categoryId: Int): List<MenuItem>
    suspend fun getItemById(itemId: Int): MenuItem
    suspend fun createItem(item: MenuItem)
    suspend fun updateItem(item: MenuItem)
    suspend fun deleteItem(itemId: Int)
    suspend fun toggleItemActive(itemId: Int)
    suspend fun itemExists(name: String, categoryId: Int): Boolean

    fun getAllMenuItemsWithVariants(): List<MenuItemWithVariants>
}

class MenuRepositoryImpl: MenuRepository {
    override suspend fun getItemsByCategory(categoryId: Int): List<MenuItem> {
        return transaction {
            MenuItems
                .select { MenuItems.categoryId eq categoryId }
                .map { menuItemRow->
                    val variants = ItemVariants
                        .select { ItemVariants.itemId eq  menuItemRow[MenuItems.id].value}
                        .map { variantRow->
                            ItemVariant(
                                size = variantRow[ItemVariants.size],
                                price = variantRow[ItemVariants.price],
                                memberPrice = variantRow[ItemVariants.memberPrice],
                                itemName = menuItemRow[MenuItems.name],
                                id = variantRow[ItemVariants.id].value,
                                itemId = variantRow[ItemVariants.itemId]
                            )
                        }

                    MenuItem(
                        id = menuItemRow[MenuItems.id].value,
                        categoryId = menuItemRow[MenuItems.categoryId],
                        name = menuItemRow[MenuItems.name],
                        description = menuItemRow[MenuItems.description],
                        variants = variants,
                        isActive = menuItemRow[MenuItems.isActive]
                    )
                }
        }
    }

    override suspend fun getItemById(itemId: Int): MenuItem {
        return transaction {
            MenuItems
                .select { MenuItems.id eq itemId }
                .singleOrNull()
                ?.let { menuItemRow ->
                    val variants = ItemVariants
                        .select { ItemVariants.itemId eq itemId }
                        .map { variantRow ->
                            ItemVariant(
                                id = variantRow[ItemVariants.id].value, // Add this
                                itemId = variantRow[ItemVariants.itemId], // Add this
                                size = variantRow[ItemVariants.size],
                                price = variantRow[ItemVariants.price],
                                memberPrice = variantRow[ItemVariants.memberPrice],
                                itemName = menuItemRow[MenuItems.name] // From parent item
                            )
                        }

                    MenuItem(
                        id = menuItemRow[MenuItems.id].value,
                        categoryId = menuItemRow[MenuItems.categoryId],
                        name = menuItemRow[MenuItems.name],
                        description = menuItemRow[MenuItems.description],
                        variants = variants,
                        isActive = menuItemRow[MenuItems.isActive],
                        discountType = menuItemRow[MenuItems.discountType],
                        discountValue = menuItemRow[MenuItems.discountValue]
                    )
                } ?: throw NoSuchElementException("Menu item with ID $itemId not found")
        }
    }

    override suspend fun createItem(item: MenuItem) {
        transaction {
            val itemId = MenuItems.insert {
                it[categoryId] = item.categoryId
                it[name] = item.name
                it[description] = item.description
                it[isActive] = item.isActive
                it[discountType] = item.discountType
                it[discountValue] = item.discountValue
            } get MenuItems.id

            item.variants.forEach { variant ->
                ItemVariants.insert {
                    it[ItemVariants.itemId] = itemId.value
                    it[size] = variant.size
                    it[price] = variant.price
                    it[memberPrice] = variant.memberPrice
                }
            }
        }
    }

    override suspend fun updateItem(item: MenuItem) {
        transaction {
            // Update the main MenuItem
            MenuItems.update({ MenuItems.id eq item.id }) {
                it[categoryId] = item.categoryId
                it[name] = item.name
                it[description] = item.description
                it[isActive] = item.isActive
                it[discountType] = item.discountType
                it[discountValue] = item.discountValue
            }

            // Delete old variants
            ItemVariants.deleteWhere { itemId eq item.id }

            // Insert new variants
            item.variants.forEach { variant ->
                ItemVariants.insert {
                    it[itemId] = item.id
                    it[size] = variant.size
                    it[price] = variant.price
                    it[memberPrice] = variant.memberPrice
                }
            }
        }
    }

    override suspend fun deleteItem(itemId: Int) {
        transaction {
            // First delete item variants
            ItemVariants.deleteWhere { ItemVariants.itemId eq itemId }

            // Then delete the menu item
            MenuItems.deleteWhere { MenuItems.id eq itemId }
        }
    }



    override suspend fun toggleItemActive(itemId: Int) {
        transaction {
            val currentStatus = MenuItems
                .select { MenuItems.id eq itemId }
                .singleOrNull()?.get(MenuItems.isActive) ?: return@transaction

            MenuItems.update({ MenuItems.id eq itemId }) {
                it[isActive] = !currentStatus
            }
        }
    }


    override suspend fun itemExists(name: String, categoryId: Int): Boolean {
        return transaction {
            MenuItems.select {
                (MenuItems.name eq name) and
                        (MenuItems.categoryId eq categoryId)
            }.count() > 0
        }
    }

    // MenuRepositoryImpl.kt
    override fun getAllMenuItemsWithVariants(): List<MenuItemWithVariants> {
        return transaction {
            (MenuItems innerJoin ItemVariants innerJoin Categories)
                .selectAll()
                .groupBy { it[MenuItems.id] }
                .map { (itemId, rows) ->
                    val firstRow = rows.first()
                    val menuItem = MenuItem(
                        id = firstRow[MenuItems.id].value,
                        categoryId = firstRow[MenuItems.categoryId],
                        name = firstRow[MenuItems.name],
                        description = firstRow[MenuItems.description],
                        isActive = firstRow[MenuItems.isActive],
                        discountType = firstRow[MenuItems.discountType],
                        discountValue = firstRow[MenuItems.discountValue],
                        variants = emptyList()
                    )

                    val variants = rows.map { row ->
                        ItemVariant(
                            id = row[ItemVariants.id].value,
                            itemId = itemId.value,
                            itemName = row[MenuItems.name], // Get name from joined table
                            size = row[ItemVariants.size],
                            price = row[ItemVariants.price],
                            memberPrice = row[ItemVariants.memberPrice]
                        )
                    }

                    MenuItemWithVariants(menuItem, variants, firstRow[Categories.name])
                }
        }
    }
}