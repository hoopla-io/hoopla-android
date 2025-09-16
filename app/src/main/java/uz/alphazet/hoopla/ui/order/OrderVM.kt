package uz.alphazet.hoopla.ui.order

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.order.OrderDetails
import uz.alphazet.data.models.order.OrderInfoData
import uz.alphazet.domain.repositories.OrderRepo
import uz.alphazet.domain.ui.BaseVM

class OrderVM(private val repo: OrderRepo) : BaseVM() {

    suspend fun validateOrder(shopId: Int, drinkId: Int): SharedFlow<UIResource<OrderDetails>> {
        return repo.validateOrder(shopId, drinkId)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

    suspend fun createOrder(
        shopId: Int,
        drinkId: Int,
        addOnId: String?
    ): SharedFlow<UIResource<OrderInfoData>> {
        return repo.createOrder(shopId, drinkId, addOnId)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

}