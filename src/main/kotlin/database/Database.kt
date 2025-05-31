package database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.sqlite.SQLiteException
import java.sql.DriverManager

object PosDatabase {

    fun init() {
        try {
            // Load SQLite JDBC driver explicitly
            Class.forName("org.sqlite.JDBC")

            // Create connection
            Database.connect(
                url = "jdbc:sqlite:pizza_pos.db",
                driver = "org.sqlite.JDBC"
            )

            // Verify connection
            DriverManager.getConnection("jdbc:sqlite:pizza_pos.db").use {
                println("✅ Database connection established")
            }

//            DataSeeder.seedInitialData()

            transaction {
                // Create tables in proper order
                SchemaUtils.create(Categories)
                SchemaUtils.create(MenuItems)
                SchemaUtils.create(ItemVariants)
                SchemaUtils.create(Orders)
                SchemaUtils.create(OrderItems)
                SchemaUtils.create(Members)
                SchemaUtils.create(RawItems)
                SchemaUtils.create(Recipes)
                SchemaUtils.create(InventoryTransactions)
                SchemaUtils.create(DailyInventory)
                println("✅ Database tables created successfully")
            }
        } catch (e: ClassNotFoundException) {
            System.err.println("❌ SQLite JDBC driver not found: ${e.message}")
        } catch (e: SQLiteException) {
            System.err.println("❌ SQLite error: ${e.message}")
        } catch (e: Exception) {
            System.err.println("❌ Database initialization failed: ${e.message}")
        }
    }

}