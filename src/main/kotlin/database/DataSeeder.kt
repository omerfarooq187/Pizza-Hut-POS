package database

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import data.model.DiscountType

object DataSeeder {
    fun seedInitialData() {
        transaction {
            SchemaUtils.create(
                Categories,
                MenuItems,
                ItemVariants,
                Orders,
                OrderItems,
                RawItems,
                Recipes,
                Members,
                InventoryTransactions
            )


            // Raw Items
            val rawItemIds = mapOf(
                "Pizza Dough" to RawItems.insertAndGetId {
                    it[name] = "Pizza Dough"; it[unit] = "kg"; it[currentStock] = 10.0; it[alertThreshold] = 3.0
                },
                "Tomato Sauce" to RawItems.insertAndGetId {
                    it[name] = "Tomato Sauce"; it[unit] = "kg"; it[currentStock] = 10.0; it[alertThreshold] = 1.0
                },
                "Cheese" to RawItems.insertAndGetId {
                    it[name] = "Cheese"; it[unit] = "kg"; it[currentStock] = 10.0; it[alertThreshold] = 2.0
                },
                "Chicken Boneless" to RawItems.insertAndGetId {
                    it[name] = "Chicken Boneless"; it[unit] = "kg"; it[currentStock] = 10.0; it[alertThreshold] = 2.0
                },
                "Ketchup Packets" to RawItems.insertAndGetId {
                    it[name] = "Ketchup Packets"; it[unit] = "pcs"; it[currentStock] = 100.0; it[alertThreshold] = 10.0
                },
                "Small Pizza Box" to RawItems.insertAndGetId {
                    it[name] = "Small Pizza Box"; it[unit] = "pcs"; it[currentStock] = 100.0; it[alertThreshold] = 10.0
                },
                "Medium Pizza Box" to RawItems.insertAndGetId {
                    it[name] = "Medium Pizza Box"; it[unit] = "pcs"; it[currentStock] = 100.0; it[alertThreshold] = 10.0
                },
                "Large Pizza Box" to RawItems.insertAndGetId {
                    it[name] = "Large Pizza Box"; it[unit] = "pcs"; it[currentStock] = 100.0; it[alertThreshold] = 10.0
                },
                "XL Pizza Box" to RawItems.insertAndGetId {
                    it[name] = "XL Pizza Box"; it[unit] = "pcs"; it[currentStock] = 100.0; it[alertThreshold] = 10.0
                },
                "Seekh Kabab" to RawItems.insertAndGetId {
                    it[name] = "Seekh Kabab"; it[unit] = "pcs"; it[currentStock] = 100.0; it[alertThreshold] = 10.0
                },
                "Pepperoni" to RawItems.insertAndGetId {
                    it[name] = "Pepperoni"; it[unit] = "pcs"; it[currentStock] = 100.0; it[alertThreshold] = 10.0
                },
                "Chicken Patty" to RawItems.insertAndGetId {
                    it[name] = "Chicken Patty"; it[unit] = "pcs"; it[currentStock] = 100.0; it[alertThreshold] = 10.0
                },
                "Chicken Zinger" to RawItems.insertAndGetId {
                    it[name] = "Chicken Zinger"; it[unit] = "pcs"; it[currentStock] = 100.0; it[alertThreshold] = 10.0
                },
                "Paratha" to RawItems.insertAndGetId {
                    it[name] = "Paratha"; it[unit] = "pcs"; it[currentStock] = 100.0; it[alertThreshold] = 10.0
                },
                "Shawarma Bread" to RawItems.insertAndGetId {
                    it[name] = "Shawarma Bread"; it[unit] = "pcs"; it[currentStock] = 100.0; it[alertThreshold] = 10.0
                },
                "Burger Bun" to RawItems.insertAndGetId {
                    it[name] = "Burger Bun"; it[unit] = "pcs"; it[currentStock] = 100.0; it[alertThreshold] = 10.0
                },
                "Wings" to RawItems.insertAndGetId {
                    it[name] = "Wings"; it[unit] = "pcs"; it[currentStock] = 100.0; it[alertThreshold] = 10.0
                },
                "Nuggets" to RawItems.insertAndGetId {
                    it[name] = "Nuggets"; it[unit] = "pcs"; it[currentStock] = 100.0; it[alertThreshold] = 10.0
                },
                "Chicken Broast Piece" to RawItems.insertAndGetId {
                    it[name] = "Chicken Broast Piece"; it[unit] = "pcs"; it[currentStock] = 100.0; it[alertThreshold] = 10.0
                },
                "Fries" to RawItems.insertAndGetId {
                    it[name] = "Fries"; it[unit] = "kg"; it[currentStock] = 10.0; it[alertThreshold] = 1.0
                },
                "Pasta" to RawItems.insertAndGetId {
                    it[name] = "Pasta"; it[unit] = "kg"; it[currentStock] = 10.0; it[alertThreshold] = 10.0
                },
                "Rice" to RawItems.insertAndGetId {
                    it[name] = "Rice"; it[unit] = "kg"; it[currentStock] = 10.0; it[alertThreshold] = 10.0
                }
            )

            // Categories
            val regularPizzaCat = Categories.insert { it[name] = "Regular Flavours" }
            val specialPizzaCat = Categories.insert { it[name] = "Special Flavours" }
            val hutSpecialCat = Categories.insert { it[name] = "Pizza Hut Specials" }
            val toppingsCat = Categories.insert { it[name] = "Extra Toppings" }

            data class MenuSeed(
                val categoryId: Int,
                val name: String,
                val description: String,
                val discountType: DiscountType?,
                val discountValue: Double?,
                val variants: List<Pair<String, Double>>,
                val ingredients: List<Pair<String, Double>>
            )

            val pizzaMenuData = listOf(
                // Regular Flavours
                MenuSeed(regularPizzaCat[Categories.id], "Chicken Tikka B.B.Q Pizza", "Smoky BBQ chicken pizza",
                    DiscountType.PERCENTAGE, 50.0,
                    listOf("S" to 1200.0, "M" to 2200.0, "L" to 2800.0, "XL" to 3400.0),
                    listOf()),

                MenuSeed(regularPizzaCat[Categories.id], "Chicken Tandoori Pizza", "Traditional tandoori flavor",
                    DiscountType.PERCENTAGE, 50.0,
                    listOf("S" to 1200.0, "M" to 2200.0, "L" to 2800.0, "XL" to 3400.0),
                    listOf()),

                MenuSeed(regularPizzaCat[Categories.id], "Chicken Achari Pizza", "Tangy pickle-marinated chicken",
                    DiscountType.PERCENTAGE, 50.0,
                    listOf("S" to 1200.0, "M" to 2200.0, "L" to 2800.0, "XL" to 3400.0),
                    listOf()),

                MenuSeed(regularPizzaCat[Categories.id], "Hot & Spicy Pizza", "Fiery red chili chicken",
                    DiscountType.PERCENTAGE, 50.0,
                    listOf("S" to 1200.0, "M" to 2200.0, "L" to 2800.0, "XL" to 3400.0),
                    listOf()),

                MenuSeed(regularPizzaCat[Categories.id], "Vegetarian Pizza", "Fresh garden vegetables",
                    DiscountType.PERCENTAGE, 50.0,
                    listOf("S" to 1200.0, "M" to 2200.0, "L" to 2800.0, "XL" to 3400.0),
                    listOf()),

                MenuSeed(regularPizzaCat[Categories.id], "Grilled Pizza", "Charcoal grilled special",
                    DiscountType.PERCENTAGE, 50.0,
                    listOf("S" to 1200.0, "M" to 2200.0, "L" to 2800.0, "XL" to 3400.0),
                    listOf()),

                // Special Flavours
                MenuSeed(specialPizzaCat[Categories.id], "Chicken Fajita Pizza", "Sizzling fajita chicken",
                    DiscountType.PERCENTAGE, 50.0,
                    listOf("S" to 1400.0, "M" to 2400.0, "L" to 3200.0, "XL" to 4000.0),
                    listOf()),

                MenuSeed(specialPizzaCat[Categories.id], "Chicken Lover Pizza", "Extra chicken topping",
                    DiscountType.PERCENTAGE, 50.0,
                    listOf("S" to 1400.0, "M" to 2400.0, "L" to 3200.0, "XL" to 4000.0),
                    listOf()),

                MenuSeed(specialPizzaCat[Categories.id], "Chicken Supreme Pizza", "Premium chicken combination",
                    DiscountType.PERCENTAGE, 50.0,
                    listOf("S" to 1400.0, "M" to 2400.0, "L" to 3200.0, "XL" to 4000.0),
                    listOf()),

                MenuSeed(specialPizzaCat[Categories.id], "Malai Boti Pizza", "Creamy malai chicken",
                    DiscountType.PERCENTAGE, 50.0,
                    listOf("S" to 1400.0, "M" to 2400.0, "L" to 3200.0, "XL" to 4000.0),
                    listOf()),

                MenuSeed(specialPizzaCat[Categories.id], "Margherita Pizza", "Classic cheese pizza",
                    DiscountType.PERCENTAGE, 50.0,
                    listOf("S" to 1400.0, "M" to 2400.0, "L" to 3200.0, "XL" to 4000.0),
                    listOf()),

                MenuSeed(specialPizzaCat[Categories.id], "Pepperoni Pizza", "Classic pepperoni",
                    DiscountType.PERCENTAGE, 50.0,
                    listOf("S" to 1400.0, "M" to 2400.0, "L" to 3200.0, "XL" to 4000.0),
                    listOf()),

                // Pizza Hut Specials
                MenuSeed(specialPizzaCat[Categories.id], "Crown Crust Pizza", "Special crown crust",
                    null, null,
                    listOf("M" to 1300.0, "L" to 1800.0, "XL" to 2200.0),
                    listOf()),

                MenuSeed(hutSpecialCat[Categories.id], "Behari Kabab Pizza", "Traditional behari kabab",
                    null, null,
                    listOf("M" to 1300.0, "L" to 1800.0, "XL" to 2200.0),
                    listOf()),

                MenuSeed(hutSpecialCat[Categories.id], "Kabab Stuffer Pizza", "Stuffed with kabab",
                    null, null,
                    listOf("M" to 1300.0, "L" to 1800.0, "XL" to 2200.0),
                    listOf()),

                MenuSeed(hutSpecialCat[Categories.id], "Special Cheese Crust Pizza", "Double cheese crust",
                    null, null,
                    listOf("M" to 1500.0, "L" to 2000.0, "XL" to 2500.0),
                    listOf()),

                // Extra Toppings
                MenuSeed(toppingsCat[Categories.id], "Extra Topping", "Additional topping",
                    null, null,
                    listOf("S" to 150.0, "M" to 200.0, "L" to 300.0, "XL" to 400.0),
                    listOf())
            )

            pizzaMenuData.forEach { item ->
                val menuItemId = MenuItems.insertAndGetId {
                    it[categoryId] = item.categoryId
                    it[name] = item.name
                    it[description] = item.description
                    it[discountType] = item.discountType
                    it[discountValue] = item.discountValue
                    it[isActive] = true
                }

                item.variants.forEach { (size, price) ->
                    val variantId = ItemVariants.insertAndGetId {
                        it[itemId] = menuItemId.value
                        it[this.size] = size
                        it[this.price] = price
                    }

                    item.ingredients.forEach { (rawName, qty) ->
                        rawItemIds[rawName]?.let { rawId ->
                            Recipes.insert {
                                it[Recipes.variantId] = variantId.value
                                it[rawItemId] = rawId.value
                                it[quantityNeeded] = qty
                            }
                        }
                    }
                }
            }

            // Category
            val burgerCat = Categories.insert { it[name] = "Burger Corner" }

// Burger Corner Menu
            val burgerMenuData = listOf(
                MenuSeed(burgerCat[Categories.id], "Chicken Patty Burger", "", null, null,
                    listOf("Regular" to 280.0, "Cheese" to 330.0),
                    listOf()),

                MenuSeed(burgerCat[Categories.id], "Zinger Burger", "", null, null,
                    listOf("Regular" to 320.0, "Cheese" to 370.0),
                    listOf()),

                MenuSeed(burgerCat[Categories.id], "Tower Burger", "", null, null,
                    listOf("Regular" to 450.0, "Cheese" to 500.0),
                    listOf()),

                MenuSeed(burgerCat[Categories.id], "Mighty Burger", "", null, null,
                    listOf("Regular" to 500.0),
                    listOf()),

                MenuSeed(burgerCat[Categories.id], "Chicken Grilled Burger", "", null, null,
                    listOf("Regular" to 500.0),
                    listOf()),

                MenuSeed(burgerCat[Categories.id], "MPH Special Burger", "", null, null,
                    listOf("Regular" to 550.0),
                    listOf())
            )

            burgerMenuData.forEach { item ->
                val menuItemId = MenuItems.insertAndGetId {
                    it[categoryId] = item.categoryId
                    it[name] = item.name
                    it[description] = item.description
                    it[discountType] = item.discountType
                    it[discountValue] = item.discountValue
                    it[isActive] = true
                }

                item.variants.forEach { (size, price) ->
                    val variantId = ItemVariants.insertAndGetId {
                        it[itemId] = menuItemId.value
                        it[this.size] = size
                        it[this.price] = price
                    }

                    item.ingredients.forEach { (rawName, qty) ->
                        rawItemIds[rawName]?.let { rawId ->
                            Recipes.insert {
                                it[Recipes.variantId] = variantId.value
                                it[rawItemId] = rawId.value
                                it[quantityNeeded] = qty
                            }
                        }
                    }
                }
            }


            // Tasty Wrap Rolls Category
            val wrapCat = Categories.insert { it[name] = "Tasty Wrap Rolls" }

            val wrapMenuData = listOf(
                MenuSeed(wrapCat[Categories.id], "Chicken Tikka Shawarma", "", null, null,
                    listOf("Regular" to 200.0, "Cheese" to 250.0),
                    listOf()),

                MenuSeed(wrapCat[Categories.id], "Zinger Shawarma", "", null, null,
                    listOf("Regular" to 250.0, "Cheese" to 300.0),
                    listOf()),

                MenuSeed(wrapCat[Categories.id], "Kababish Shawarma", "", null, null,
                    listOf("Regular" to 300.0, "Cheese" to 350.0),
                    listOf()),

                MenuSeed(wrapCat[Categories.id], "Chicken Pratha Roll", "", null, null,
                    listOf("Regular" to 250.0, "Cheese" to 300.0),
                    listOf()),

                MenuSeed(wrapCat[Categories.id], "Zinger Pratha Roll", "", null, null,
                    listOf("Regular" to 300.0, "Cheese" to 350.0),
                    listOf()),

                MenuSeed(wrapCat[Categories.id], "Kababish Pratha Roll", "", null, null,
                    listOf("Regular" to 350.0, "Cheese" to 400.0),
                    listOf())
            )

            wrapMenuData.forEach { item ->
                val menuItemId = MenuItems.insertAndGetId {
                    it[categoryId] = item.categoryId
                    it[name] = item.name
                    it[description] = item.description
                    it[discountType] = item.discountType
                    it[discountValue] = item.discountValue
                    it[isActive] = true
                }

                item.variants.forEach { (size, price) ->
                    val variantId = ItemVariants.insertAndGetId {
                        it[itemId] = menuItemId.value
                        it[this.size] = size
                        it[this.price] = price
                    }

                    item.ingredients.forEach { (rawName, qty) ->
                        rawItemIds[rawName]?.let { rawId ->
                            Recipes.insert {
                                it[Recipes.variantId] = variantId.value
                                it[rawItemId] = rawId.value
                                it[quantityNeeded] = qty
                            }
                        }
                    }
                }
            }

// Crispy Crunch Category
            val crispyCat = Categories.insert { it[name] = "Crispy Crunch" }

            val crispyMenuData = listOf(
                MenuSeed(crispyCat[Categories.id], "Crispy Wings", "", null, null,
                    listOf("6PCS" to 320.0, "12PCS" to 600.0),
                    listOf()),

                MenuSeed(crispyCat[Categories.id], "Hot Shots", "", null, null,
                    listOf("6PCS" to 350.0, "12PCS" to 650.0),
                    listOf()),

                MenuSeed(crispyCat[Categories.id], "Chicken Nuggets", "", null, null,
                    listOf("6PCS" to 300.0, "12PCS" to 600.0),
                    listOf()),

                MenuSeed(crispyCat[Categories.id], "Drum Sticks", "", null, null,
                    listOf("2PCS" to 500.0),
                    listOf()),

                MenuSeed(crispyCat[Categories.id], "Chicken Piece", "", null, null,
                    listOf("1PCS" to 250.0),
                    listOf()),

                MenuSeed(crispyCat[Categories.id], "Half Broast", "", null, null,
                    listOf("4PCS" to 850.0),
                    listOf()),

                MenuSeed(crispyCat[Categories.id], "Full Broast", "", null, null,
                    listOf("8PCS" to 1700.0),
                    listOf())
            )


            crispyMenuData.forEach { item ->
                val menuItemId = MenuItems.insertAndGetId {
                    it[categoryId] = item.categoryId
                    it[name] = item.name
                    it[description] = item.description
                    it[discountType] = item.discountType
                    it[discountValue] = item.discountValue
                    it[isActive] = true
                }

                item.variants.forEach { (size, price) ->
                    val variantId = ItemVariants.insertAndGetId {
                        it[itemId] = menuItemId.value
                        it[this.size] = size
                        it[this.price] = price
                    }

                    item.ingredients.forEach { (rawName, qty) ->
                        rawItemIds[rawName]?.let { rawId ->
                            Recipes.insert {
                                it[Recipes.variantId] = variantId.value
                                it[rawItemId] = rawId.value
                                it[quantityNeeded] = qty
                            }
                        }
                    }
                }
            }

            // Fries and Pasta Category
            val friesCat = Categories.insert { it[name] = "Fries and Pasta" }

            val friesMenuData = listOf(
                MenuSeed(friesCat[Categories.id], "Plain Fries", "", null, null,
                    listOf("M" to 220.0, "L" to 300.0, "F" to 450.0),
                    listOf()),

                MenuSeed(friesCat[Categories.id], "Masala Fries", "", null, null,
                    listOf("M" to 240.0, "L" to 320.0, "F" to 480.0),
                    listOf()),

                MenuSeed(friesCat[Categories.id], "Garlic Mayo Fries", "", null, null,
                    listOf("M" to 300.0, "L" to 370.0, "F" to 550.0),
                    listOf()),

                MenuSeed(friesCat[Categories.id], "Chicken Loaded Fries", "", null, null,
                    listOf("M" to 350.0, "L" to 650.0, "F" to 850.0),
                    listOf()),

                MenuSeed(friesCat[Categories.id], "Kababish Loaded Fries", "", null, null,
                    listOf("M" to 450.0, "L" to 800.0, "F" to 1000.0),
                    listOf()),

                MenuSeed(friesCat[Categories.id], "Oven Baked Pasta", "", null, null,
                    listOf("M" to 400.0, "L" to 700.0),
                    listOf()),

                MenuSeed(friesCat[Categories.id], "Creamy Pasta", "", null, null,
                    listOf("M" to 450.0, "L" to 850.0),
                    listOf())
            )

            friesMenuData.forEach { item ->
                val menuItemId = MenuItems.insertAndGetId {
                    it[categoryId] = item.categoryId
                    it[name] = item.name
                    it[description] = item.description
                    it[discountType] = item.discountType
                    it[discountValue] = item.discountValue
                    it[isActive] = true
                }

                item.variants.forEach { (size, price) ->
                    val variantId = ItemVariants.insertAndGetId {
                        it[itemId] = menuItemId.value
                        it[this.size] = size
                        it[this.price] = price
                    }

                    item.ingredients.forEach { (rawName, qty) ->
                        rawItemIds[rawName]?.let { rawId ->
                            Recipes.insert {
                                it[Recipes.variantId] = variantId.value
                                it[rawItemId] = rawId.value
                                it[quantityNeeded] = qty
                            }
                        }
                    }
                }
            }

            // Special Deals Category
            val dealsCat = Categories.insert { it[name] = "Deals" }

            val dealsMenuData = listOf(
                MenuSeed(dealsCat[Categories.id], "Deal 1", "1 Zinger Burger, 1 Plain Fries (S), 1 Drink NR 345ml", null, null,
                    listOf("Regular" to 500.0), listOf()),

                MenuSeed(dealsCat[Categories.id], "Deal 2", "1 Zinger Burger, 1 Chicken Piece, 1 Drink NR 345ml", null, null,
                    listOf("Regular" to 600.0), listOf()),

                MenuSeed(dealsCat[Categories.id], "Deal 3", "6 Crispy Wings, 6 Chic Nuggets, 1 Drink 500ml", null, null,
                    listOf("Regular" to 800.0), listOf()),

                MenuSeed(dealsCat[Categories.id], "Deal 4", "1 BBQ Pizza (S), 5 Chic Nuggets, 1 Drink 500ml", null, null,
                    listOf("Regular" to 850.0), listOf()),

                MenuSeed(dealsCat[Categories.id], "Deal 5", "2 Zinger Burgers, 5 Crispy Wings, 1 Drink 500ml", null, null,
                    listOf("Regular" to 1050.0), listOf()),

                MenuSeed(dealsCat[Categories.id], "Deal 6", "2 Zinger Burgers, 2 Chic Shawarmas, 1 Drink 500ml", null, null,
                    listOf("Regular" to 1100.0), listOf()),

                MenuSeed(dealsCat[Categories.id], "Deal 7", "1 BBQ Pizza (S), 2 Zinger Burgers, 1 Drink 500ml", null, null,
                    listOf("Regular" to 1300.0), listOf()),

                MenuSeed(dealsCat[Categories.id], "Deal 8", "1 BBQ Pizza (M), 5 Chic Nuggets, 1 Drink 1 LTR", null, null,
                    listOf("Regular" to 1500.0), listOf()),

                MenuSeed(dealsCat[Categories.id], "Deal 9", "4 Chic Shawarmas, 10 Chic Nuggets, 1 Drink 1 LTR", null, null,
                    listOf("Regular" to 1500.0), listOf()),

                MenuSeed(dealsCat[Categories.id], "Deal 10", "2 BBQ Pizza (S), 1 Plain Fries (L), 1 Drink 1 LTR", null, null,
                    listOf("Regular" to 1600.0), listOf()),

                MenuSeed(dealsCat[Categories.id], "Deal 11", "4 Zinger Burgers, 1 Plain Fries (L), 1 Drink 1.5 LTR", null, null,
                    listOf("Regular" to 1750.0), listOf()),

                MenuSeed(dealsCat[Categories.id], "Deal 12", "1 BBQ Pizza (M), 2 Zinger Burgers, 1 Drink 1 LTR", null, null,
                    listOf("Regular" to 1800.0), listOf()),

                MenuSeed(dealsCat[Categories.id], "Deal 13", "1 BBQ Pizza (M), 1 Fajita Pizza (S), 1 Drink 1.5 LTR", null, null,
                    listOf("Regular" to 1850.0), listOf()),

                MenuSeed(dealsCat[Categories.id], "Deal 14", "1 BBQ Pizza (L), 1 Plain Fries (L), 1 Drink 1.5 LTR", null, null,
                    listOf("Regular" to 1900.0), listOf()),

                MenuSeed(dealsCat[Categories.id], "Deal 15", "1 BBQ Pizza (L), 10 Crispy Wings, 1 Drink 1.5 LTR", null, null,
                    listOf("Regular" to 2200.0), listOf()),

                MenuSeed(dealsCat[Categories.id], "Deal 16", "6 Zinger Burgers, 1 Plain Fries (L), 1 Drink 1.5 LTR", null, null,
                    listOf("Regular" to 2400.0), listOf()),

                MenuSeed(dealsCat[Categories.id], "Deal 17", "2 BBQ Pizza (M), 1 Plain Fries (L), 1 Drink 1.5 LTR", null, null,
                    listOf("Regular" to 2650.0), listOf()),

                MenuSeed(dealsCat[Categories.id], "Deal 18", "2 BBQ Pizza (L), 1 Plain Fries (F), 1 Drink 1.5 LTR", null, null,
                    listOf("Regular" to 3200.0), listOf())
            )

            val icecreamCat = Categories.insert { it[name] = "Ice Cream" }

            val icecreamCatData = listOf(
                MenuSeed(icecreamCat[Categories.id], "Special Scoop (1)", "", null, null,
                    listOf("Single cup" to 120.0),
                    listOf()),
                MenuSeed(icecreamCat[Categories.id], "Special Scoop (2)", "", null, null,
                    listOf("Single cup" to 200.0),
                    listOf()),
                MenuSeed(icecreamCat[Categories.id], "Special Scoop (3)", "", null, null,
                    listOf("Single cup" to 350.0),
                    listOf()),
            )

            icecreamCatData.forEach { item ->
                val menuItemId = MenuItems.insertAndGetId {
                    it[categoryId] = item.categoryId
                    it[name] = item.name
                    it[description] = item.description
                    it[discountType] = item.discountType
                    it[discountValue] = item.discountValue
                    it[isActive] = true
                }

                item.variants.forEach { (size, price) ->
                    val variantId = ItemVariants.insertAndGetId {
                        it[itemId] = menuItemId.value
                        it[this.size] = size
                        it[this.price] = price
                    }

                    item.ingredients.forEach { (rawName, qty) ->
                        rawItemIds[rawName]?.let { rawId ->
                            Recipes.insert {
                                it[Recipes.variantId] = variantId.value
                                it[rawItemId] = rawId.value
                                it[quantityNeeded] = qty
                            }
                        }
                    }
                }
            }

            dealsMenuData.forEach { item ->
                val menuItemId = MenuItems.insertAndGetId {
                    it[categoryId] = item.categoryId
                    it[name] = item.name
                    it[description] = item.description
                    it[discountType] = item.discountType
                    it[discountValue] = item.discountValue
                    it[isActive] = true
                }

                item.variants.forEach { (size, price) ->
                    val variantId = ItemVariants.insertAndGetId {
                        it[itemId] = menuItemId.value
                        it[this.size] = size
                        it[this.price] = price
                    }

                    item.ingredients.forEach { (rawName, qty) ->
                        rawItemIds[rawName]?.let { rawId ->
                            Recipes.insert {
                                it[Recipes.variantId] = variantId.value
                                it[rawItemId] = rawId.value
                                it[quantityNeeded] = qty
                            }
                        }
                    }
                }
            }

            val birthdayCat = Categories.insert { it[name] = "Birthday Party Deals" }

            val birthdayDealsData = listOf(
                MenuSeed(birthdayCat[Categories.id], "Birthday Deal 1", "1 Pound Cake, 1 Special Pizza (L), 2 Zinger Burgers, 10 Chic Nuggets, 1 Plain Fries (L), 1 Drink 2.25 ml", null, null,
                    listOf("Regular" to 4000.0), listOf()),

                MenuSeed(birthdayCat[Categories.id], "Birthday Deal 2", "1 Pound Cake, 2 Special Pizza (L), 2 Zinger Burgers, 20 Crispy Wings, 1 Plain Fries (F), 2 Drinks 1.5 LTR", null, null,
                    listOf("Regular" to 7000.0), listOf()),

                MenuSeed(birthdayCat[Categories.id], "Birthday Deal 3", "2 Pound Cake, 1 BBQ Pizza (XL), 1 Special Pizza (L), 7 Chic Patty Burgers, 15 Crispy Wings, 2 Plain Fries (F), 2 Drinks 1.5 LTR", null, null,
                    listOf("Regular" to 9500.0), listOf()),

                MenuSeed(birthdayCat[Categories.id], "Birthday Deal 4", "3 Pound Cake (Special), 2 Special Pizza (XL), 5 Zinger Burgers, 5 Patty Burgers, 20 Chic Nuggets, 20 Crispy Wings, 3 Plain Fries, 3 Drink 1.5 LTR", null, null,
                    listOf("Regular" to 14500.0), listOf()),
            )

            birthdayDealsData.forEach { item ->
                val menuItemId = MenuItems.insertAndGetId {
                    it[categoryId] = item.categoryId
                    it[name] = item.name
                    it[description] = item.description
                    it[discountType] = item.discountType
                    it[discountValue] = item.discountValue
                    it[isActive] = true
                }

                item.variants.forEach { (size, price) ->
                    val variantId = ItemVariants.insertAndGetId {
                        it[itemId] = menuItemId.value
                        it[this.size] = size
                        it[this.price] = price
                    }

                    item.ingredients.forEach { (rawName, qty) ->
                        rawItemIds[rawName]?.let { rawId ->
                            Recipes.insert {
                                it[Recipes.variantId] = variantId.value
                                it[rawItemId] = rawId.value
                                it[quantityNeeded] = qty
                            }
                        }
                    }
                }
            }


            val familyDealsCat = Categories.insert { it[name] = "Family Deals" }

            val familyDealsMenuData = listOf(
                MenuSeed(familyDealsCat[Categories.id], "Family Deal 1", "1 BBQ Pizza (M), 3 Chic Sharwarmas, 1 Plain Fries (F), 1 Drink 1.5 LTR", null, null,
                    listOf("Regular" to 2300.0), listOf()),
            MenuSeed(familyDealsCat[Categories.id], "Family Deal 2", "2 BBQ Pizza (S), 12 Crispy Wings, 1 Plain Fries (F), 1 Drink 1.5 LTR", null, null,
                listOf("Regular" to 2500.0), listOf()),
                MenuSeed(familyDealsCat[Categories.id], "Family Deal 3", "1 BBQ Pizza (L), 3 Zinger Burgers, 1 Plain Fries (F), 1 Drink 1.5 LTR", null, null,
                    listOf("Regular" to 3000.0), listOf()),

                MenuSeed(familyDealsCat[Categories.id], "Family Deal 4", "1 BBQ Pizza (XL), 15 Chic Nuggets, 1 Plain Fries (F), 1 Drink 2.25 LTR", null, null,
                    listOf("Regular" to 3200.0), listOf())
            )

            familyDealsMenuData.forEach { item ->
                val menuItemId = MenuItems.insertAndGetId {
                    it[categoryId] = item.categoryId
                    it[name] = item.name
                    it[description] = item.description
                    it[discountType] = item.discountType
                    it[discountValue] = item.discountValue
                    it[isActive] = true
                }

                item.variants.forEach { (size, price) ->
                    val variantId = ItemVariants.insertAndGetId {
                        it[itemId] = menuItemId.value
                        it[this.size] = size
                        it[this.price] = price
                    }

                    item.ingredients.forEach { (rawName, qty) ->
                        rawItemIds[rawName]?.let { rawId ->
                            Recipes.insert {
                                it[Recipes.variantId] = variantId.value
                                it[rawItemId] = rawId.value
                                it[quantityNeeded] = qty
                            }
                        }
                    }
                }
            }

            val superDealsCat = Categories.insert { it[name] = "Super Deals" }
            val superDealsMenuData = listOf(
                MenuSeed(superDealsCat[Categories.id], "Super Deal 1", "3 Small Pizzas, Tikka Tandoori Spicy, 1 Drink 1.5 LTR - Free", null, null,
                    listOf("Regular" to 1900.0), listOf()),
                MenuSeed(superDealsCat[Categories.id], "Super Deal 2", "3 Medium Pizzas, Tikka Tandoori Spicy, 1 Drink 1.5 LTR - Free", null, null,
                    listOf("Regular" to 3300.0), listOf()),
                MenuSeed(superDealsCat[Categories.id], "Super Deal 3", "3 Large Pizzas, Tikka Tandoori Spicy, 1 Drink 2.25 ml - Free", null, null,
                    listOf("Regular" to 4200.0), listOf()),

                MenuSeed(superDealsCat[Categories.id], "Super Deal 4", "3 Extra Large Pizzas, Tikka Tandoori Spicy, 1 Drink 2.25 ml - Free", null, null,
                    listOf("Regular" to 5000.0), listOf())
            )

            superDealsMenuData.forEach { item ->
                val menuItemId = MenuItems.insertAndGetId {
                    it[categoryId] = item.categoryId
                    it[name] = item.name
                    it[description] = item.description
                    it[discountType] = item.discountType
                    it[discountValue] = item.discountValue
                    it[isActive] = true
                }

                item.variants.forEach { (size, price) ->
                    val variantId = ItemVariants.insertAndGetId {
                        it[itemId] = menuItemId.value
                        it[this.size] = size
                        it[this.price] = price
                    }

                    item.ingredients.forEach { (rawName, qty) ->
                        rawItemIds[rawName]?.let { rawId ->
                            Recipes.insert {
                                it[Recipes.variantId] = variantId.value
                                it[rawItemId] = rawId.value
                                it[quantityNeeded] = qty
                            }
                        }
                    }
                }
            }

            val mandraMourghPalao = Categories.insert{ it[name] = "Mandra Mourgh Palaou"}

            val mandraMourghPalaoData = listOf(
                MenuSeed(mandraMourghPalao[Categories.id], "Chicken Palaou Single", "", null, null,
                    listOf("Regular" to 490.0),
                    listOf()),
                MenuSeed(mandraMourghPalao[Categories.id], "Chicken Palaou Single Choice", "", null, null,
                    listOf("Regular" to 520.0),
                    listOf()),
                MenuSeed(mandraMourghPalao[Categories.id], "Chicken Palaou Single (Without Kabab)", "", null, null,
                    listOf("Regular" to 400.0),
                    listOf()),
                MenuSeed(mandraMourghPalao[Categories.id], "Chicken Double Palaou", "", null, null,
                    listOf("Regular" to 650.0),
                    listOf()),
                MenuSeed(mandraMourghPalao[Categories.id], "Chicken Double Palaou Choice", "", null, null,
                    listOf("Regular" to 700.0),
                    listOf()),
                MenuSeed(mandraMourghPalao[Categories.id], "Chicken Double Palaou (Without Kabab)", "", null, null,
                    listOf("Regular" to 600.0),
                    listOf()),
                MenuSeed(mandraMourghPalao[Categories.id], "Simple Palaou Kabab", "", null, null,
                    listOf("Regular" to 330.0),
                    listOf()),
                MenuSeed(mandraMourghPalao[Categories.id], "Simple Palaou", "", null, null,
                    listOf("Regular" to 250.0),
                    listOf()),
                MenuSeed(mandraMourghPalao[Categories.id], "12 Shami Kabab", "", null, null,
                    listOf("Regular" to 480.0),
                    listOf()),
                MenuSeed(mandraMourghPalao[Categories.id], "12 Chicken Steam pcs", "", null, null,
                    listOf("Regular" to 2200.0),
                    listOf()),
                MenuSeed(mandraMourghPalao[Categories.id], "Steam Chargha", "", null, null,
                    listOf("Regular" to 2000.0),
                    listOf()),
                MenuSeed(mandraMourghPalao[Categories.id], "Chicken Fried Rice", "", null, null,
                    listOf("Regular" to 450.0),
                    listOf()),
                MenuSeed(mandraMourghPalao[Categories.id], "Masala Fried Rice", "", null, null,
                    listOf("Regular" to 400.0),
                    listOf()),
            )

            mandraMourghPalaoData.forEach { item ->
                val menuItemId = MenuItems.insertAndGetId {
                    it[categoryId] = item.categoryId
                    it[name] = item.name
                    it[description] = item.description
                    it[discountType] = item.discountType
                    it[discountValue] = item.discountValue
                    it[isActive] = true
                }

                item.variants.forEach { (size, price) ->
                    val variantId = ItemVariants.insertAndGetId {
                        it[itemId] = menuItemId.value
                        it[this.size] = size
                        it[this.price] = price
                    }

                    item.ingredients.forEach { (rawName, qty) ->
                        rawItemIds[rawName]?.let { rawId ->
                            Recipes.insert {
                                it[Recipes.variantId] = variantId.value
                                it[rawItemId] = rawId.value
                                it[quantityNeeded] = qty
                            }
                        }
                    }
                }
            }
        }
    }
}