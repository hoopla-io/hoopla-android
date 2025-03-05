package uz.alphazet.data.models

import uz.alphazet.data.rv.BaseItem

data class PartnerItemData(
    val description: String?,
    val id: Int?,
    val logoUrl: String?,
    val name: String?
) : BaseItem {
    override val uniqueId: String
        get() = id.toString()
}