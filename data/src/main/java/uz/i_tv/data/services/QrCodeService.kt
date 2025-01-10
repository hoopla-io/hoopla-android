package uz.i_tv.data.services

import retrofit2.http.GET
import uz.i_tv.data.BaseResponse
import uz.i_tv.data.models.QRCodeAccessData

interface QrCodeService {

    @GET("v1/user/generate-qr-code")
    suspend fun generateQRCode(): BaseResponse<QRCodeAccessData>

}