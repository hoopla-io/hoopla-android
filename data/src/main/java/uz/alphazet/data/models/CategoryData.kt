package uz.alphazet.data.models

import uz.alphazet.data.rv.BaseItem

data class CategoryData(
    val id: Int?,
    val name: String?,
    val imageUrl: String?
) : BaseItem {
    override val uniqueId: String = id.toString()
}