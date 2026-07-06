package uz.alphazet.data.services

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.UserData

interface ProfileService {

    @GET("v1/user/get-me")
    suspend fun getMe(): BaseResponse<UserData>

    @GET("v1/user/edit-me")
    suspend fun editMe(): BaseResponse<UserData>

    @POST("v1/user/logout")
    suspend fun logout(
        @Query("refreshToken") refreshToken: String? = null
    ): BaseResponse<Any>

    @PUT("v1/user/update-me")
    suspend fun updateMe(@Body body: RequestBody): BaseResponse<Any>

    @DELETE("v1/user/deactivate")
    suspend fun deactivate(): BaseResponse<Any>

}