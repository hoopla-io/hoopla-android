package uz.alphazet.hoopla.ui.order

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.UserData
import uz.alphazet.data.models.order.CheckOutInfo
import uz.alphazet.data.models.order.ModifierItemData
import uz.alphazet.data.models.order.OrderDetails
import uz.alphazet.data.models.order.OrderInfoData
import uz.alphazet.domain.repositories.OrderRepo
import uz.alphazet.domain.repositories.ProfileRepo
import uz.alphazet.domain.ui.BaseVM
import uz.alphazet.domain.ui.load

class OrderVM(
    private val repo: OrderRepo,
    private val profileRepo: ProfileRepo
) : BaseVM() {

    var defaultPrice: Double? = null

    private val userDataEmitter: MutableStateFlow<UIResource<UserData>> =
        MutableStateFlow(UIResource.Loading)
    val userDataFlow: StateFlow<UIResource<UserData>> get() = userDataEmitter

    init {
        getUser()
    }

    fun getUser() {
        launch { userDataEmitter.load { profileRepo.getMe() } }
    }

    suspend fun validateOrder(shopId: Int, drinkId: Int): SharedFlow<UIResource<OrderDetails>> {
        return repo.validateOrder(shopId, drinkId)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

    suspend fun createOrder(
        shopId: Int,
        drinkId: Int,
        modifiers: ArrayList<ModifierItemData>
    ): SharedFlow<UIResource<OrderInfoData>> {
        return repo.createOrder(shopId, drinkId, modifiers)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

    suspend fun createOrderRahmat(
        shopId: Int,
        drinkId: Int,
        modifiers: ArrayList<ModifierItemData>,
        useCashback: Boolean,
        cashbackAmount: Double
    ): SharedFlow<UIResource<CheckOutInfo>> {
        return repo.createOrderRahmat(shopId, drinkId, modifiers, useCashback, cashbackAmount)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

}