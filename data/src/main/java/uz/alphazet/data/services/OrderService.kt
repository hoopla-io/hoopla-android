package uz.alphazet.data.services

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.order.CheckOutInfo
import uz.alphazet.data.models.order.OrderDetails
import uz.alphazet.data.models.order.OrderInfoData

interface OrderService {

    @POST("v1/user/orders/validate-order")
    suspend fun validateOrder(
        @Body body: RequestBody
    ): BaseResponse<OrderDetails>

    @POST("v1/user/orders/create")
    suspend fun createOrder(
        @Body body: RequestBody
    ): BaseResponse<OrderInfoData>

    @POST("v1/user/orders/create-rahmat")
    suspend fun createOrderRahmat(
        @Body body: RequestBody
    ): BaseResponse<CheckOutInfo>

    @GET("v1/user/orders/create-rahmat")
    suspend fun getPaymentStatus(): BaseResponse<Any>

}