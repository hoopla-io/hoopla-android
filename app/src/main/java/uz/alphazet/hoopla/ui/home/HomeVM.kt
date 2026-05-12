package uz.alphazet.hoopla.ui.home

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.CategoryData
import uz.alphazet.data.models.FeedbackDetail
import uz.alphazet.data.models.LoyaltyItemData
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.data.models.StoryItemData
import uz.alphazet.data.models.UserData
import uz.alphazet.domain.repositories.CategoryRepo
import uz.alphazet.domain.repositories.HomeRepo
import uz.alphazet.domain.repositories.ProfileRepo
import uz.alphazet.domain.repositories.StoryRepo
import uz.alphazet.domain.ui.BaseVM
import uz.alphazet.domain.ui.load

class HomeVM(
    private val homeRepo: HomeRepo,
    private val profileRepo: ProfileRepo,
    private val categoryRepo: CategoryRepo,
    private val storyRepo: StoryRepo
) : BaseVM() {

    private val pendingFeedbackEmitter: MutableStateFlow<UIResource<FeedbackDetail>> =
        MutableStateFlow(UIResource.Loading)
    val pendingFeedbackFlow: StateFlow<UIResource<FeedbackDetail>> get() = pendingFeedbackEmitter

    private val userDataEmitter: MutableStateFlow<UIResource<UserData>> =
        MutableStateFlow(UIResource.Loading)
    val userDataFlow: StateFlow<UIResource<UserData>> get() = userDataEmitter

    private val categoriesEmitter: MutableStateFlow<UIResource<List<CategoryData>>> =
        MutableStateFlow(UIResource.Loading)
    val categoriesFlow: StateFlow<UIResource<List<CategoryData>>> get() = categoriesEmitter

    private val storiesEmitter: MutableStateFlow<UIResource<List<StoryItemData>>> =
        MutableStateFlow(UIResource.Loading)
    val storiesFlow: StateFlow<UIResource<List<StoryItemData>>> get() = storiesEmitter

    fun getUser() {
        launch { userDataEmitter.load { profileRepo.getMe() } }
    }

    fun getCategories() {
        launch { categoriesEmitter.load { categoryRepo.getCategories() } }
    }

    fun getStories() {
        launch { storiesEmitter.load { storyRepo.getStories() } }
    }

    suspend fun getLoyaltyCard(): SharedFlow<UIResource<List<LoyaltyItemData>>> {
        return homeRepo.getLoyaltyCard()
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

    suspend fun getNearShops(
        lat: Double,
        long: Double,
        name: String?,
        categoryId: Int? = null,
    ): SharedFlow<UIResource<List<ShopItemData>>> {
        return homeRepo.getNearShops(lat, long, name, categoryId)
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