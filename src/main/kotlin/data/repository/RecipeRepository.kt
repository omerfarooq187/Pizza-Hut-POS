package data.repository

import data.model.ItemVariant
import data.model.Recipe
import database.Recipes
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

interface RecipeRepository {
    suspend fun addRecipe(recipe: Recipe)
    suspend fun getRecipesForVariant(variantId: Int): List<Recipe>
    suspend fun getRecipesForRawItem(rawItemId: Int): List<Recipe>
    suspend fun deleteRecipe(variantId: Int, rawItemId: Int)
}

// data/repository/RecipeRepositoryImpl.kt
class RecipeRepositoryImpl : RecipeRepository {
    override suspend fun addRecipe(recipe: Recipe) {
        transaction {
            Recipes.insert {
                it[variantId] = recipe.variantId
                it[rawItemId] = recipe.rawItemId
                it[quantityNeeded] = recipe.quantityNeeded
            }
        }
    }

    override suspend fun getRecipesForVariant(variantId: Int): List<Recipe> {
        return transaction {
            Recipes.select { Recipes.variantId eq variantId }
                .map {
                    Recipe(
                        variantId = it[Recipes.variantId],
                        rawItemId = it[Recipes.rawItemId],
                        quantityNeeded = it[Recipes.quantityNeeded]
                    )
                }
        }
    }

    override suspend fun getRecipesForRawItem(rawItemId: Int): List<Recipe> {
        return transaction {
            Recipes.select { Recipes.rawItemId eq rawItemId }
                .map { it.toRecipe() }
        }
    }

    override suspend fun deleteRecipe(variantId: Int, rawItemId: Int) {
        transaction {
            Recipes.deleteWhere {
                (Recipes.variantId eq variantId) and
                        (Recipes.rawItemId eq rawItemId)
            }
        }
    }


    private fun ItemVariant.copy(recipes: List<Recipe>?): ItemVariant {
        return ItemVariant(
            id = this.id,
            itemId = this.itemId,
            size = this.size,
            price = this.price,
            recipes = recipes
        )
    }
    private fun ResultRow.toRecipe() = Recipe(
        variantId = this[Recipes.variantId],
        rawItemId = this[Recipes.rawItemId],
        quantityNeeded = this[Recipes.quantityNeeded]
    )

}