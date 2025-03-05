package uz.alphazet.data.services

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.SubscriptionItemData

interface SubscriptionService {

    @GET("v1/subscriptions")
    suspend fun getSubscriptions(): BaseResponse<List<SubscriptionItemData>>

    @POST("v1/subscriptions/buy")
    suspend fun buySubscription(
        @Body requestBody: RequestBody
    ): BaseResponse<Any>

}