package uz.alphazet.data.services

import retrofit2.http.GET
import retrofit2.http.Query
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.NotificationItemData

interface NotificationService {

    @GET("v1/notifications/get-list")
    suspend fun getNotifications(
        @Query("page") page: Int,
        @Query("itemsPerPage") itemsPerPage: Int
    ): BaseResponse<List<NotificationItemData>>

}