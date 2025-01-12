package uz.i_tv.data.services

import retrofit2.http.GET
import uz.i_tv.data.BaseResponse
import uz.i_tv.data.models.SubscriptionItemData

interface SubscriptionService {

    @GET("v1/subscriptions")
    suspend fun getSubscriptions(): BaseResponse<List<SubscriptionItemData>>

}