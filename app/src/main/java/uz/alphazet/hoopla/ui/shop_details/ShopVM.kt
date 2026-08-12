package uz.alphazet.hoopla.ui.shop_details

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.ShopData
import uz.alphazet.data.models.ShopDrinksData
import uz.alphazet.data.models.order.OrderDetails
import uz.alphazet.domain.repositories.OrderRepo
import uz.alphazet.domain.repositories.ShopRepo
import uz.alphazet.domain.ui.BaseVM

class ShopVM(
    private val repo: ShopRepo,
    private val orderRepo: OrderRepo
) : BaseVM() {

    /**
     * Asks what a drink needs before it can be ordered. The menu payload carries no
     * "has modifiers" flag, so this is the only way to tell a one-tap drink from one whose
     * options have to be picked first. Deliberately not [uz.alphazet.hoopla.ui.order.OrderVM],
     * whose init block would fire a profile request from every shop screen.
     */
    suspend fun validateOrder(shopId: Int, drinkId: Int): SharedFlow<UIResource<OrderDetails>> {
        return orderRepo.validateOrder(shopId, drinkId)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

    suspend fun getShopDetail(shopId: Int): SharedFlow<UIResource<ShopData>> {
        return repo.getShopDetail(shopId)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

    suspend fun getShopDrinks(shopId: Int): SharedFlow<UIResource<ShopDrinksData>> {
        return repo.getShopDrinks(shopId)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

}