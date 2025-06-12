package uz.alphazet.hoopla.ui.qr_code

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.DailyDrinksStatData
import uz.alphazet.data.models.OrderItemData
import uz.alphazet.data.models.QRCodeAccessData
import uz.alphazet.data.models.UserData
import uz.alphazet.domain.repositories.ProfileRepo
import uz.alphazet.domain.repositories.QRCodeRepo
import uz.alphazet.domain.ui.BaseVM
import uz.alphazet.domain.ui.load

class QRCodeVM(private val repo: QRCodeRepo, private val profileRepo: ProfileRepo) : BaseVM() {

    private val qrCodeDataEmitter: MutableStateFlow<UIResource<QRCodeAccessData>> =
        MutableStateFlow(UIResource.Loading)
    val qrCodeDataFlow: StateFlow<UIResource<QRCodeAccessData>> get() = qrCodeDataEmitter

    private val drinksStatDataEmitter: MutableStateFlow<UIResource<DailyDrinksStatData>> =
        MutableStateFlow(UIResource.Loading)
    val drinksStatDataFlow: StateFlow<UIResource<DailyDrinksStatData>> get() = drinksStatDataEmitter

    private val userDataEmitter: MutableStateFlow<UIResource<UserData>> =
        MutableStateFlow(UIResource.Loading)
    val userDataFlow: StateFlow<UIResource<UserData>> get() = userDataEmitter

    init {
        generateQRCode()
        getDrinksStat()
        getUser()
    }

    fun getUser() {
        launch {
            userDataEmitter.load { profileRepo.getMe() }
        }
    }

    fun generateQRCode() {
        launch {
            qrCodeDataEmitter.load { repo.generateQRCode() }
        }
    }

    fun getDrinksStat() {
        launch {
            drinksStatDataEmitter.load { repo.getDrinksStat() }
        }
    }

    suspend fun getOrders(): SharedFlow<UIResource<List<OrderItemData>>> {
        return repo.getOrders()
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

}