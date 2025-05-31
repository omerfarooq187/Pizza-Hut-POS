package data.repository

import data.model.Category
import database.Categories
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update


interface CategoryRepository {
    suspend fun getAllCategories(): List<Category>
    suspend fun createCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(categoryId: Int)
    suspend fun categoryExists(name: String, excludeId: Int? = null): Boolean
}


class CategoryRepositoryImpl: CategoryRepository {
    override suspend fun getAllCategories(): List<Category> = transaction {
        Categories.selectAll().map {
            Category(
                id = it[Categories.id],
                name = it[Categories.name]
            )
        }.toList()
    }

    override suspend fun createCategory(category: Category) {
        transaction {
            Categories.insert {
                it[name] = category.name
            }
        }
    }

    override suspend fun updateCategory(category: Category) {
        transaction {
            Categories.update({ Categories.id eq category.id}) {
                it[name] = category.name
            }
        }
    }

    override suspend fun deleteCategory(categoryId: Int) {
        transaction {
            Categories.deleteWhere {
                Categories.id eq categoryId
            }
        }
    }

    override suspend fun categoryExists(name: String, excludeId: Int?): Boolean = transaction {
        val query = Categories.name eq name
        val excludeQuery = excludeId?.let { Categories.id neq it }
        Categories.select {
            query and (excludeQuery?: Op.TRUE)
        }.count() > 0
    }

}