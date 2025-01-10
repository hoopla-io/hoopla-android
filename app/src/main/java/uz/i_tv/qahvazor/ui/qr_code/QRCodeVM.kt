package uz.i_tv.qahvazor.ui.qr_code

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import uz.i_tv.data.UIResource
import uz.i_tv.data.models.QRCodeAccessData
import uz.i_tv.domain.repositories.QRCodeRepo
import uz.i_tv.domain.ui.BaseVM

class QRCodeVM(private val repo: QRCodeRepo) : BaseVM() {

    suspend fun generateQRCode(): SharedFlow<UIResource<QRCodeAccessData>> {
        return repo.generateQRCode()
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

}