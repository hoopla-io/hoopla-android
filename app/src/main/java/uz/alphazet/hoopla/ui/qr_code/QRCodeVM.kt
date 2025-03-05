package uz.alphazet.hoopla.ui.qr_code

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.OrderItemData
import uz.alphazet.data.models.QRCodeAccessData
import uz.alphazet.domain.repositories.QRCodeRepo
import uz.alphazet.domain.ui.BaseVM

class QRCodeVM(private val repo: QRCodeRepo) : BaseVM() {

    suspend fun generateQRCode(): SharedFlow<UIResource<QRCodeAccessData>> {
        return repo.generateQRCode()
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

    suspend fun getOrders(): SharedFlow<UIResource<List<OrderItemData>>> {
        return repo.getOrders()
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

}