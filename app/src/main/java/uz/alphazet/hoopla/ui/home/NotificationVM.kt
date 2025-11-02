package uz.alphazet.hoopla.ui.home

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.NotificationDetail
import uz.alphazet.data.models.NotificationItemData
import uz.alphazet.domain.repositories.NotificationDataSource
import uz.alphazet.domain.repositories.NotificationRepo
import uz.alphazet.domain.ui.BaseVM
import uz.alphazet.domain.ui.load

class NotificationVM(
    private val dataSource: NotificationDataSource,
    private val repo: NotificationRepo
) : BaseVM() {

    private val notificationDetailEmitter: MutableStateFlow<UIResource<NotificationDetail>> =
        MutableStateFlow(UIResource.Loading)
    val notificationDetailFlow: StateFlow<UIResource<NotificationDetail>> get() = notificationDetailEmitter

    fun getNotificationDetail(id: Int) {
        launch { notificationDetailEmitter.load { repo.getNotificationDetail(id) } }
    }

    fun getNotificationsPager(): SharedFlow<PagingData<NotificationItemData>> =
        Pager(
            PagingConfig(10, initialLoadSize = 10),
            pagingSourceFactory = { dataSource.create() }
        ).flow.cachedIn(viewModelScope)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)

}