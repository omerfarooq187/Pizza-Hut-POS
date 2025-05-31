package di

import data.repository.*
import database.PosDatabase
import org.koin.dsl.module
import presentation.viewmodel.*
import service.InventoryService

val appModule = module {
    single(createdAtStart = true) {
        PosDatabase.init()
    }
    single<CategoryRepository> { CategoryRepositoryImpl() }
    single { CategoryViewModel() }
    single<MenuRepository> { MenuRepositoryImpl() }
    single { MenuItemViewModel() }
    single<OrderRepository> { OrderRepositoryImpl() }
    single { OrderViewModel() }
    single { ReportViewModel() }
    single { DashboardViewModel() }
    single<RawItemRepository> { RawItemRepositoryImpl() }
    single<RecipeRepository> { RecipeRepositoryImpl() }
    single { InventoryService(get(), get(), get())  }
    single { InventoryViewModel() }
    single { RecipeViewModel() }
    single<MemberRepository> { MemberRepositoryImpl() }
    single { MemberViewModel() }

}
