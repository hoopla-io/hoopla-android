package uz.alphazet.hoopla.ui.profile.subscriptions

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.SubscriptionItemData
import uz.alphazet.domain.repositories.SubscriptionRepo
import uz.alphazet.domain.ui.BaseVM

class SubscriptionVM(private val repo: SubscriptionRepo) : BaseVM() {

    suspend fun getSubscriptions(): SharedFlow<UIResource<List<SubscriptionItemData>>> {
        return repo.getSubscriptions()
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

    suspend fun buySubscription(subscriptionId: Int): SharedFlow<UIResource<Any>> {
        return repo.buySubscription(subscriptionId)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

}