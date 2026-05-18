package uz.alphazet.hoopla.ui.orders

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.DailyDrinksStatData
import uz.alphazet.data.models.QRCodeAccessData
import uz.alphazet.data.models.UserData
import uz.alphazet.data.models.order.OrderInfo
import uz.alphazet.data.models.order.OrderItemData
import uz.alphazet.domain.repositories.OrderHistoryDataSource
import uz.alphazet.domain.repositories.ProfileRepo
import uz.alphazet.domain.repositories.OrdersRepo
import uz.alphazet.domain.ui.BaseVM
import uz.alphazet.domain.ui.load

class OrdersVM(
    private val repo: OrdersRepo, private val profileRepo: ProfileRepo,
    private val dataSource: OrderHistoryDataSource
) : BaseVM() {

    private val qrCodeDataEmitter: MutableStateFlow<UIResource<QRCodeAccessData>> =
        MutableStateFlow(UIResource.Loading)
    val qrCodeDataFlow: StateFlow<UIResource<QRCodeAccessData>> get() = qrCodeDataEmitter

    private val drinksStatDataEmitter: MutableStateFlow<UIResource<DailyDrinksStatData>> =
        MutableStateFlow(UIResource.Loading)
    val drinksStatDataFlow: StateFlow<UIResource<DailyDrinksStatData>> get() = drinksStatDataEmitter

    private val userDataEmitter: MutableStateFlow<UIResource<UserData>> =
        MutableStateFlow(UIResource.Loading)
    val userDataFlow: StateFlow<UIResource<UserData>> get() = userDataEmitter
    private val orderInfoEmitter: MutableStateFlow<UIResource<OrderInfo>> =
        MutableStateFlow(UIResource.Loading)
    val orderInfoFlow: StateFlow<UIResource<OrderInfo>> get() = orderInfoEmitter

    private val pickupQrEmitter: MutableStateFlow<UIResource<QRCodeAccessData>> =
        MutableStateFlow(UIResource.Loading)
    val pickupQrFlow: StateFlow<UIResource<QRCodeAccessData>> get() = pickupQrEmitter

    init {
//        generateQRCode()
//        getDrinksStat()
//        getUser()
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

    fun getOrderHistoryPager(): SharedFlow<PagingData<OrderItemData>> =
        Pager(
            PagingConfig(10, initialLoadSize = 10),
            pagingSourceFactory = { dataSource.create() }
        ).flow.cachedIn(viewModelScope)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)

    fun getOrderInfo(id: Int) {
        launch {
            orderInfoEmitter.load { repo.getOrderInfo(id) }
        }
    }

    fun getOrderPickupQR(id: Int) {
        launch {
            pickupQrEmitter.load { repo.getOrderPickupQR(id) }
        }
    }

    suspend fun cancelOrder(id: Int): SharedFlow<UIResource<Any>> {
        return repo.cancelOrder(id)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

}