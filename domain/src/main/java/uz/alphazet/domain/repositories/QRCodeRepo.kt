package uz.alphazet.domain.repositories

import uz.alphazet.data.services.QrCodeService
import uz.alphazet.domain.network.BaseRepo

class QRCodeRepo(private val api: QrCodeService) : BaseRepo() {

    suspend fun generateQRCode() = handle { api.generateQRCode() }
    suspend fun getOrderInfo(id: Int) = handle { api.getOrderInfo(id) }
    suspend fun cancelOrder(id: Int) = handleFlow { api.cancelOrder(id) }
    suspend fun getDrinksStat() = handle { api.getDrinksStat() }

}