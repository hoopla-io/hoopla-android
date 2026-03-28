package uz.alphazet.hoopla.ui.home

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.FeedbackDetail
import uz.alphazet.data.models.LoyaltyItemData
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.domain.repositories.HomeRepo
import uz.alphazet.domain.ui.BaseVM
import uz.alphazet.domain.ui.load

class HomeVM(private val homeRepo: HomeRepo) : BaseVM() {

    private val pendingFeedbackEmitter: MutableStateFlow<UIResource<FeedbackDetail>> =
        MutableStateFlow(UIResource.Loading)
    val pendingFeedbackFlow: StateFlow<UIResource<FeedbackDetail>> get() = pendingFeedbackEmitter

    suspend fun getLoyaltyCard(): SharedFlow<UIResource<List<LoyaltyItemData>>> {
        return homeRepo.getLoyaltyCard()
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

    suspend fun getNearShops(
        lat: Double,
        long: Double,
        name: String?,
    ): SharedFlow<UIResource<List<ShopItemData>>> {
        return homeRepo.getNearShops(lat, long, name)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

    private val submitFeedbackEmitter: MutableStateFlow<UIResource<Any>> =
        MutableStateFlow(UIResource.Loading)
    val submitFeedbackFlow: StateFlow<UIResource<Any>> get() = submitFeedbackEmitter

    fun getPendingFeedbacks() {
        launch { pendingFeedbackEmitter.load { homeRepo.getPendingFeedbacks() } }
    }

    fun submitFeedback(orderId: Int, rating: Int, comment: String?) {
        launch { submitFeedbackEmitter.load { homeRepo.submitFeedback(orderId, rating, comment) } }
    }

}