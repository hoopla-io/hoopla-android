package uz.i_tv.data.services

import retrofit2.http.GET
import retrofit2.http.Query
import uz.i_tv.data.BaseResponse
import uz.i_tv.data.models.PaymentServiceCheckOutData
import uz.i_tv.data.models.PaymentServiceItemData

interface PaymentService {

    @GET("v1/user/pay/services")
    suspend fun getPaymentServices(): BaseResponse<List<PaymentServiceItemData>>

    @GET("v1/user/pay/top-up")
    suspend fun topUpViaPayService(
        @Query("id") id: Int,
        @Query("amount") amount: Double
    ): BaseResponse<PaymentServiceCheckOutData>

}