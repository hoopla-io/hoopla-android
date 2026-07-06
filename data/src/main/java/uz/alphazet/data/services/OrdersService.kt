package uz.alphazet.data.services

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.DailyDrinksStatData
import uz.alphazet.data.models.QRCodeAccessData
import uz.alphazet.data.models.order.OrderInfo
import uz.alphazet.data.models.order.OrderItemData

interface OrdersService {

    @GET("v1/user/generate-qr-code")
    suspend fun generateQRCode(): BaseResponse<QRCodeAccessData>

    @GET("v1/user/orders/orders-list")
    suspend fun getOrders(
        @Query("page") page: Int,
        @Query("itemsPerPage") itemsPerPage: Int
    ): BaseResponse<List<OrderItemData>>

    @GET("v1/user/orders/active")
    suspend fun getActiveOrders(): BaseResponse<List<OrderItemData>>

    @GET("v1/user/orders/{id}")
    suspend fun getOrderInfo(@Path("id") id: Int): BaseResponse<OrderInfo>

    @GET("v1/user/orders/{id}/pickup-qr")
    suspend fun getOrderPickupQR(@Path("id") id: Int): BaseResponse<QRCodeAccessData>

    @POST("v1/user/orders/{id}/cancel")
    suspend fun cancelOrder(@Path("id") id: Int): BaseResponse<Any>

    @GET("v1/user/orders/drinks-stat")
    suspend fun getDrinksStat(): BaseResponse<DailyDrinksStatData>

}