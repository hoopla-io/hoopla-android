package uz.alphazet.hoopla.ui.partner

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.PartnerItemData
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.domain.repositories.PartnerRepo
import uz.alphazet.domain.ui.BaseVM

class PartnerVM(private val partnerRepo: PartnerRepo) : BaseVM() {

    /** The brand itself — only needed when the screen was opened by id alone. */
    suspend fun getPartner(partnerId: Int): SharedFlow<UIResource<PartnerItemData>> {
        return partnerRepo.getPartner(partnerId)
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
