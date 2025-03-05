package uz.alphazet.data.services

import retrofit2.http.GET
import retrofit2.http.Query
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.PartnerData
import uz.alphazet.data.models.ShopItemData

interface PartnerService {

    @GET("v1/partners/partner")
    suspend fun getPartnerDetail(
        @Query("id") partnerId: Int
    ): BaseResponse<PartnerData>

    @GET("v1/shops/partner-shops")
    suspend fun getPartnerShops(
        @Query("partnerId") partnerId: Int
    ): BaseResponse<List<ShopItemData>>

}