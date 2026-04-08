package uz.alphazet.domain.repositories

import uz.alphazet.data.services.NotificationService
import uz.alphazet.domain.network.BaseRepo

class NotificationRepo(private val service: NotificationService) : BaseRepo() {

    suspend fun getNotificationDetail(id: Int, language: String = "ru") = handle {
        service.getNotificationDetail(id, language)
    }

    suspend fun markRead() = handleFlow {
        service.markRead()
    }

}