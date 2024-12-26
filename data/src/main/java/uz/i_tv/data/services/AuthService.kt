package uz.i_tv.data.services

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import uz.i_tv.data.AnyResponse

interface AuthService {

    @FormUrlEncoded
    @POST("v1/auth/login")
    suspend fun login(
        @Field("phoneNumber") phoneNumber: String
    ): AnyResponse


}