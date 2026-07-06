package uz.alphazet.domain.repositories

import uz.alphazet.data.services.OrdersService
import uz.alphazet.domain.network.BaseRepo

class OrdersRepo(private val api: OrdersService) : BaseRepo() {

    suspend fun generateQRCode() = handle { api.generateQRCode() }
    suspend fun getActiveOrders() = handle { api.getActiveOrders() }
    suspend fun getOrderInfo(id: Int) = handle { api.getOrderInfo(id) }
    suspend fun getOrderPickupQR(id: Int) = handle { api.getOrderPickupQR(id) }
    suspend fun cancelOrder(id: Int) = handleFlow { api.cancelOrder(id) }
    suspend fun getDrinksStat() = handle { api.getDrinksStat() }

}