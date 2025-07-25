package uz.alphazet.data.models

data class OrderInfoData(
    val id: Int?,
    val partnerName: String?,
    val shopName: String?,
    val purchasedAt: String?,
    val purchasedAtUnix: Long?,
    val drinkName: String?,
    val orderStatus: String?
)
