package uz.alphazet.data.models.order

import uz.alphazet.data.rv.BaseItem

data class OrderItemData(
    val id: Int?,
    val partnerName: String?,
    val shopName: String?,
    val purchasedAt: String?,
    val purchasedAtUnix: Long?,
    val drinkName: String?,
    val orderStatus: String?,
    val productPrice: Double?,
    val fiscalLink: String?
) : BaseItem {
    override val uniqueId: String
        get() = id.toString()
}