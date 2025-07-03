package uz.alphazet.data.services

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.POST
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.OrderInfoData

interface OrderService {

    @POST("v1/user/orders/create")
    suspend fun createOrder(
        @Body body: RequestBody
    ): BaseResponse<OrderInfoData>

}