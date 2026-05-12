package uz.alphazet.data.services

import retrofit2.http.GET
import retrofit2.http.Query
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.ShopData
import uz.alphazet.data.models.ShopDrinksData

interface ShopService {

    @GET("v1/shops/shop")
    suspend fun getShopDetail(
        @Query("shopId") shopId: Int
    ): BaseResponse<ShopData>

    @GET("v1/shops/drinks")
    suspend fun getShopDrinks(
        @Query("shopId") shopId: Int
    ): BaseResponse<ShopDrinksData>

}