package uz.i_tv.qahvazor.ui.partner_details

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import uz.i_tv.data.UIResource
import uz.i_tv.data.models.PartnerData
import uz.i_tv.data.models.ShopItemData
import uz.i_tv.domain.repositories.PartnerRepo
import uz.i_tv.domain.ui.BaseVM

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