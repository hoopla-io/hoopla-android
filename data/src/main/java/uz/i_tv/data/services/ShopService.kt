package uz.i_tv.data.services

import retrofit2.http.GET
import retrofit2.http.Query
import uz.i_tv.data.BaseResponse
import uz.i_tv.data.models.ShopData

interface ShopService {

    @GET("v1/shops/shop")
    suspend fun getShopDetail(
        @Query("shopId") shopId: Int
    ): BaseResponse<ShopData>

}