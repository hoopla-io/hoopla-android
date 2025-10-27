package uz.alphazet.hoopla.ui.home

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.models.NotificationItemData
import uz.alphazet.domain.repositories.NotificationDataSource
import uz.alphazet.domain.ui.BaseVM

class NotificationVM(private val dataSource: NotificationDataSource) : BaseVM() {

    fun getNotificationsPager(): SharedFlow<PagingData<NotificationItemData>> =
        Pager(
            PagingConfig(10, initialLoadSize = 10),
            pagingSourceFactory = { dataSource.create() }
        ).flow.cachedIn(viewModelScope)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)

}