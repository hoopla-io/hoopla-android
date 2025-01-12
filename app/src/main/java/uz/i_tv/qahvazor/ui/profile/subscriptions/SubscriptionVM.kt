package uz.i_tv.qahvazor.ui.profile.subscriptions

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import uz.i_tv.data.UIResource
import uz.i_tv.data.models.SubscriptionItemData
import uz.i_tv.domain.repositories.SubscriptionRepo
import uz.i_tv.domain.ui.BaseVM

class SubscriptionVM(private val repo: SubscriptionRepo) : BaseVM() {

    suspend fun getSubscriptions(): SharedFlow<UIResource<List<SubscriptionItemData>>> {
        return repo.getSubscriptions()
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

}