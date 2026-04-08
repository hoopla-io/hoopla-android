package uz.alphazet.domain.repositories

import androidx.paging.PagingState
import uz.alphazet.data.models.NotificationItemData
import uz.alphazet.data.services.NotificationService
import uz.alphazet.domain.network.BasePagingDataSource

class NotificationDataSource(
    private val service: NotificationService,
    private val language: String = "ru"
) : BasePagingDataSource<NotificationItemData>() {

    override fun getRefreshKey(state: PagingState<Int, NotificationItemData>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, NotificationItemData> =
        handle {
            service.getNotifications(params.key ?: 1, params.loadSize, language)
        }

    fun create(language: String) = NotificationDataSource(service, language)

}