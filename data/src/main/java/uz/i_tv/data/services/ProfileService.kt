package uz.i_tv.data.services

import retrofit2.http.GET
import uz.i_tv.data.BaseResponse
import uz.i_tv.data.models.UserData

interface ProfileService {

    @GET("v1/user/get-me")
    suspend fun getMe(): BaseResponse<UserData>

    @GET("v1/user/logout")
    suspend fun logout(): BaseResponse<Any>

}