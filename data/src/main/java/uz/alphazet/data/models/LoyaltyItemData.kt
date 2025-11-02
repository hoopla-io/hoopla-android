package uz.alphazet.data.models

import uz.alphazet.data.rv.BaseItem

data class LoyaltyItemData(
    val drinkIndex: Int,
    val isFilled: Boolean,
    val isFree: Boolean
) : BaseItem {
    override val uniqueId: String = drinkIndex.toString()
}
