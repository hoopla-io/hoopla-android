package uz.alphazet.data.services

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.POST
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.giftcard.GiftCardData

interface GiftCardService {

    @POST("v1/user/gift-cards/redeem")
    suspend fun redeem(
        @Body body: RequestBody
    ): BaseResponse<GiftCardData>

}