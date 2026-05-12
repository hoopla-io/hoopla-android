package uz.alphazet.data.models

import uz.alphazet.data.rv.BaseItem

data class ShopDrinksData(
    val categories: List<ShopCategoryData>?
)

data class ShopCategoryData(
    val id: Int?,
    val name: String?,
    val drinks: List<DrinkItemData>?
) : BaseItem {
    override val uniqueId: String
        get() = id?.toString() ?: name.orEmpty()
}