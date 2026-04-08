package uz.alphazet.data.services

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import uz.alphazet.data.AnyResponse
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.NotificationDetail
import uz.alphazet.data.models.NotificationItemData

interface NotificationService {

    @GET("v1/notifications/list")
    suspend fun getNotifications(
        @Query("page") page: Int,
        @Query("itemsPerPage") itemsPerPage: Int,
        @Query("language") language: String = "ru"
    ): BaseResponse<List<NotificationItemData>>

    @GET("v1/notifications/show")
    suspend fun getNotificationDetail(
        @Query("notificationId") notificationId: Int,
        @Query("language") language: String = "ru"
    ): BaseResponse<NotificationDetail>

    @POST("v1/notifications/mark-read")
    suspend fun markRead(): AnyResponse

}