package uz.alphazet.data.services

import retrofit2.http.GET
import retrofit2.http.Query
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.ShopData

interface ShopService {

    @GET("v1/shops/shop")
    suspend fun getShopDetail(
        @Query("shopId") shopId: Int
    ): BaseResponse<ShopData>

}