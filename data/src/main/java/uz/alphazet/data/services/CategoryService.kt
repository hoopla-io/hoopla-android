package uz.alphazet.data.services

import retrofit2.http.GET
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.CategoryData

interface CategoryService {

    @GET("v1/categories/list")
    suspend fun getCategories(): BaseResponse<List<CategoryData>>

}