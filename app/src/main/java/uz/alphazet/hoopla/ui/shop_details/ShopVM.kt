package uz.alphazet.hoopla.ui.shop_details

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.ShopData
import uz.alphazet.domain.repositories.ShopRepo
import uz.alphazet.domain.ui.BaseVM

class ShopVM(private val repo: ShopRepo) : BaseVM() {

    suspend fun getShopDetail(shopId: Int): SharedFlow<UIResource<ShopData>> {
        return repo.getShopDetail(shopId)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

}