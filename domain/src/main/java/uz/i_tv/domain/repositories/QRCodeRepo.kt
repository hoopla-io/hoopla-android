package uz.i_tv.domain.repositories

import uz.i_tv.data.services.QrCodeService
import uz.i_tv.domain.network.BaseRepo

class QRCodeRepo(private val api: QrCodeService) : BaseRepo() {

    suspend fun generateQRCode() = handleFlow {
        api.generateQRCode()
    }

}