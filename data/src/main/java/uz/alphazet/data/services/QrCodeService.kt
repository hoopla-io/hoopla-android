package uz.alphazet.data.services

import retrofit2.http.GET
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.DailyDrinksStatData
import uz.alphazet.data.models.order.OrderItemData
import uz.alphazet.data.models.QRCodeAccessData

interface QrCodeService {

    @GET("v1/user/generate-qr-code")
    suspend fun generateQRCode(): BaseResponse<QRCodeAccessData>

    @GET("v1/user/orders/orders-list")
    suspend fun getOrders(): BaseResponse<List<OrderItemData>>

    @GET("v1/user/orders/drinks-stat")
    suspend fun getDrinksStat(): BaseResponse<DailyDrinksStatData>

}