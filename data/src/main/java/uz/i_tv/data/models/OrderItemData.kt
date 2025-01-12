package uz.i_tv.data.models

import uz.i_tv.data.rv.BaseItem

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