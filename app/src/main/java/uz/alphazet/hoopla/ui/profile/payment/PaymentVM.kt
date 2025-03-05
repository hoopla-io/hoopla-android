package uz.alphazet.hoopla.ui.profile.payment

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.PaymentServiceCheckOutData
import uz.alphazet.data.models.PaymentServiceItemData
import uz.alphazet.domain.repositories.PaymentServiceRepo
import uz.alphazet.domain.ui.BaseVM
import uz.alphazet.domain.ui.load

class PaymentVM(private val repo: PaymentServiceRepo) : BaseVM() {

    private val topUpDataEmitter: MutableSharedFlow<UIResource<PaymentServiceCheckOutData>> =
        MutableSharedFlow()
    val topUpDataFlow: SharedFlow<UIResource<PaymentServiceCheckOutData>> get() = topUpDataEmitter

    suspend fun getPaymentServices(): SharedFlow<UIResource<List<PaymentServiceItemData>>> {
        return repo.getPaymentServices()
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

    fun topUpViaPayService(id: Int, amount: Double) {
        launch {
            topUpDataEmitter.load { repo.topUpViaPayService(id, amount) }
        }
    }

}