package uz.alphazet.hoopla.ui.partner_details

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.PartnerData
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.domain.repositories.PartnerRepo
import uz.alphazet.domain.ui.BaseVM

class PartnerVM(private val repo: PartnerRepo) : BaseVM() {

    suspend fun getPartnerDetail(partnerId: Int): SharedFlow<UIResource<PartnerData>> {
        return repo.getPartnerDetail(partnerId)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

    suspend fun getPartnerShops(partnerId: Int): SharedFlow<UIResource<List<ShopItemData>>> {
        return repo.getPartnerShops(partnerId)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

}