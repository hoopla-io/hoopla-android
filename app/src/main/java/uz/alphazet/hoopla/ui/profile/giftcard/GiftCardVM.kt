package uz.alphazet.hoopla.ui.profile.giftcard

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.giftcard.GiftCardData
import uz.alphazet.domain.repositories.GiftCardRepo
import uz.alphazet.domain.ui.BaseVM

class GiftCardVM(private val repo: GiftCardRepo) : BaseVM() {

    suspend fun redeem(code: String): SharedFlow<UIResource<GiftCardData>> {
        return repo.redeem(code)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

}