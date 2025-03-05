package uz.i_tv.data.models

import uz.i_tv.data.rv.BaseItem

data class PaymentServiceItemData(
    val id: Int?,
    val logoUrl: String?,
    val name: String?
) : BaseItem {
    override val uniqueId: String
        get() = id.toString()
}