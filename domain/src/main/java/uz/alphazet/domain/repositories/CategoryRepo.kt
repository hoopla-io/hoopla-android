package uz.alphazet.domain.repositories

import uz.alphazet.data.services.CategoryService
import uz.alphazet.domain.network.BaseRepo

class CategoryRepo(private val service: CategoryService) : BaseRepo() {

    suspend fun getCategories() = handle {
        service.getCategories()
    }

}