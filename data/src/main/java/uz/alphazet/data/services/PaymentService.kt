package uz.alphazet.data.services

import retrofit2.http.GET
import retrofit2.http.Query
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.PaymentServiceCheckOutData
import uz.alphazet.data.models.PaymentServiceItemData

interface PaymentService {

    @GET("v1/user/pay/services")
    suspend fun getPaymentServices(): BaseResponse<List<PaymentServiceItemData>>

    @GET("v1/user/pay/top-up")
    suspend fun topUpViaPayService(
        @Query("id") id: Int,
        @Query("amount") amount: Double
    ): BaseResponse<PaymentServiceCheckOutData>

}