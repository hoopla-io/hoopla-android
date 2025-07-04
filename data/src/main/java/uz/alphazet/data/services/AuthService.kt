package uz.alphazet.data.services

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.AccessTokenData
import uz.alphazet.data.models.ConfirmSMSData
import uz.alphazet.data.models.LoginSessionData

interface AuthService {

    @POST("v1/auth/login")
    suspend fun sendSMS(
        @Body body: RequestBody
    ): BaseResponse<LoginSessionData>

    @POST("v1/auth/resend-sms")
    suspend fun resendSMS(
        @Body body: RequestBody
    ): BaseResponse<LoginSessionData>

    @POST("v1/auth/confirm-sms")
    suspend fun confirmSMS(
        @Body body: RequestBody
    ): BaseResponse<ConfirmSMSData>

    @PATCH("v1/user/refresh-token")
    suspend fun refreshToken(
        @Query("refreshToken") refreshToken: String
    ): BaseResponse<AccessTokenData>

}