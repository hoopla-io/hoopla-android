package uz.alphazet.domain.repositories

import androidx.paging.PagingState
import uz.alphazet.data.models.order.OrderHistoryItemData
import uz.alphazet.data.services.OrdersService
import uz.alphazet.domain.network.BasePagingDataSource

class OrderHistoryDataSource(private val service: OrdersService) :
    BasePagingDataSource<OrderHistoryItemData>() {

    override fun getRefreshKey(state: PagingState<Int, OrderHistoryItemData>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, OrderHistoryItemData> =
        handle {
            service.getOrderHistory(params.key ?: 1, params.loadSize)
        }

    fun create() = OrderHistoryDataSource(service)

}
