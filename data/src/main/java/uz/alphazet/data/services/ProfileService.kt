package uz.alphazet.data.services

import retrofit2.http.GET
import retrofit2.http.POST
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.UserData

interface ProfileService {

    @GET("v1/user/get-me")
    suspend fun getMe(): BaseResponse<UserData>

    @POST("v1/user/logout")
    suspend fun logout(): BaseResponse<Any>

}