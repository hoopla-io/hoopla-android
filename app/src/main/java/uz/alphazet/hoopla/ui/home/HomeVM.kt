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
import uz.alphazet.data.models.UserData
import uz.alphazet.domain.repositories.HomeRepo
import uz.alphazet.domain.repositories.ProfileRepo
import uz.alphazet.domain.ui.BaseVM
import uz.alphazet.domain.ui.load

class HomeVM(
    private val homeRepo: HomeRepo,
    private val profileRepo: ProfileRepo
) : BaseVM() {

    private val pendingFeedbackEmitter: MutableStateFlow<UIResource<FeedbackDetail>> =
        MutableStateFlow(UIResource.Loading)
    val pendingFeedbackFlow: StateFlow<UIResource<FeedbackDetail>> get() = pendingFeedbackEmitter

    private val userDataEmitter: MutableStateFlow<UIResource<UserData>> =
        MutableStateFlow(UIResource.Loading)
    val userDataFlow: StateFlow<UIResource<UserData>> get() = userDataEmitter

    fun getUser() {
        launch { userDataEmitter.load { profileRepo.getMe() } }
    }

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

    fun getPendingFeedbacks() {
        launch { pendingFeedbackEmitter.load { homeRepo.getPendingFeedbacks() } }
    }

    suspend fun submitFeedback(
        orderId: Int,
        rating: Int,
        comment: String?
    ): SharedFlow<UIResource<Any>> {
        return homeRepo.submitFeedback(orderId, rating, comment)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

}