package uz.i_tv.data.services

import retrofit2.http.GET
import retrofit2.http.Query
import uz.i_tv.data.BaseResponse
import uz.i_tv.data.models.PartnerItemData
import uz.i_tv.data.models.ShopItemData

interface HomeService {

    @GET("v1/partners")
    suspend fun getPartners(): BaseResponse<List<PartnerItemData>>

    @GET("v1/shops/near-shops")
    suspend fun getNearShops(
        @Query("lat") lat: Double,
        @Query("long") long: Double
    ): BaseResponse<List<ShopItemData>>

}