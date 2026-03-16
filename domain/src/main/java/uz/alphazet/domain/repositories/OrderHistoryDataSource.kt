package uz.alphazet.domain.repositories

import androidx.paging.PagingState
import uz.alphazet.data.models.order.OrderItemData
import uz.alphazet.data.services.QrCodeService
import uz.alphazet.domain.network.BasePagingDataSource

class OrderHistoryDataSource(private val service: QrCodeService) :
    BasePagingDataSource<OrderItemData>() {

    override fun getRefreshKey(state: PagingState<Int, OrderItemData>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, OrderItemData> =
        handle {
            service.getOrders(params.key ?: 1, params.loadSize)
        }

    fun create() = OrderHistoryDataSource(service)

}