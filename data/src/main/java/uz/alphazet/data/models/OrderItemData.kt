package uz.alphazet.data.models

import uz.alphazet.data.rv.BaseItem

data class OrderItemData(
    val id: Int?,
    val partnerName: String?,
    val purchasedAt: String?,
    val purchasedAtUnix: Long?,
    val shopName: String?
) : BaseItem {
    override val uniqueId: String
        get() = id.toString()
}