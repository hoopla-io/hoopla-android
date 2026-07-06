package uz.alphazet.data.services

import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.DeviceSessionData

interface DevicesService {

    @GET("v1/user/devices")
    suspend fun getDevices(): BaseResponse<List<DeviceSessionData>>

    @DELETE("v1/user/devices/{id}")
    suspend fun revokeDevice(@Path("id") id: Int): BaseResponse<Any>

}
