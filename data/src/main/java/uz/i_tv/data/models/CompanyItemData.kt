package uz.i_tv.data.models

import uz.i_tv.data.rv.BaseItem

data class CompanyItemData(
    val description: String?,
    val id: Int?,
    val imageUrl: String?,
    val name: String?
) : BaseItem {
    override val uniqueId: String
        get() = id.toString()
}