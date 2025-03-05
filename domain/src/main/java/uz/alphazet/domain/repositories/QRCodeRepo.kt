package uz.alphazet.domain.repositories

import uz.alphazet.data.services.QrCodeService
import uz.alphazet.domain.network.BaseRepo

class QRCodeRepo(private val api: QrCodeService) : BaseRepo() {

    suspend fun generateQRCode() = handleFlow {
        api.generateQRCode()
    }

    suspend fun getOrders() = handleFlow {
        api.getOrders()
    }

}