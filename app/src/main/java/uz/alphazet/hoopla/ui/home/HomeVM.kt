package uz.alphazet.hoopla.ui.home

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.PartnerItemData
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.domain.repositories.HomeRepo
import uz.alphazet.domain.ui.BaseVM

class HomeVM(private val homeRepo: HomeRepo) : BaseVM() {

    suspend fun getPartners(): SharedFlow<UIResource<List<PartnerItemData>>> {
        return homeRepo.getPartners()
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

}