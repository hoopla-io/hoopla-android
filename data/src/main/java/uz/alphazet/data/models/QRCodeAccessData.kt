package uz.alphazet.data.models

data class QRCodeAccessData(
    val token: String?,
    val expiresAt: Long?,
    val orderId: Int?
)
