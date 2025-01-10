package uz.i_tv.qahvazor.ui.shop_details

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import uz.i_tv.data.UIResource
import uz.i_tv.data.models.ShopData
import uz.i_tv.domain.repositories.ShopRepo
import uz.i_tv.domain.ui.BaseVM

class ShopVM(private val repo: ShopRepo) : BaseVM() {

    suspend fun getShopDetail(shopId: Int): SharedFlow<UIResource<ShopData>> {
        return repo.getShopDetail(shopId)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

}