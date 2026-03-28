package uz.alphazet.data.services

import retrofit2.http.GET
import retrofit2.http.Query
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.FeedbackDetail
import uz.alphazet.data.models.LoyaltyItemData
import uz.alphazet.data.models.ShopItemData

interface HomeService {

    @GET("v1/user/orders/loyalty-card")
    suspend fun getLoyaltyCard(): BaseResponse<List<LoyaltyItemData>>

    @GET("v1/shops/near-shops")
    suspend fun getNearShops(
        @Query("lat") lat: Double,
        @Query("long") long: Double,
        @Query("name") name: String?,
    ): BaseResponse<List<ShopItemData>>

    @GET("v1/orders/feedbacks/pending")
    suspend fun getPendingFeedbacks(): BaseResponse<FeedbackDetail>

}