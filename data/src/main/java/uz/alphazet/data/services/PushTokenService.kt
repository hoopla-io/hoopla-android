package uz.alphazet.data.services

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.POST
import uz.alphazet.data.BaseResponse

interface PushTokenService {

    @POST("v1/user/push-token")
    suspend fun registerPushToken(
        @Body body: RequestBody
    ): BaseResponse<Any>

    // Plain @DELETE does not allow a request body in Retrofit, so @HTTP is used
    // to send the token identifying which registration to remove.
    @HTTP(method = "DELETE", path = "v1/user/push-token", hasBody = true)
    suspend fun unregisterPushToken(
        @Body body: RequestBody
    ): BaseResponse<Any>

}
