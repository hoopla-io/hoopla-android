package uz.alphazet.hoopla.ui.search

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.PartnerItemData
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.domain.repositories.PartnerRepo
import uz.alphazet.domain.ui.BaseVM

class SearchVM(private val partnerRepo: PartnerRepo) : BaseVM() {

    suspend fun getPartners(name: String?): SharedFlow<UIResource<List<PartnerItemData>>> {
        return partnerRepo.getPartners(name)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

    suspend fun getPartnerShops(
        partnerId: Int,
        lat: Double?,
        long: Double?
    ): SharedFlow<UIResource<List<ShopItemData>>> {
        return partnerRepo.getPartnerShops(partnerId, lat, long)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

}